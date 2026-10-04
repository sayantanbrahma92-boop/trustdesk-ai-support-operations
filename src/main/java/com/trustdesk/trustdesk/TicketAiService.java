package com.trustdesk.trustdesk;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class TicketAiService {
    private final TicketService ticketService;
    private final AiRetrievalClient aiClient;
    private final AiRunTraceRepository traceRepository;
    private final DraftRecordRepository draftRepository;

    public TicketAiService(
            TicketService ticketService,
            AiRetrievalClient aiClient,
            AiRunTraceRepository traceRepository,
            DraftRecordRepository draftRepository) {
        this.ticketService = ticketService;
        this.aiClient = aiClient;
        this.traceRepository = traceRepository;
        this.draftRepository = draftRepository;
    }

    public Optional<AiRetrievalClient.TriageResponse> triage(String ticketId) {
        return ticketService.findById(ticketId).map(ticket -> {
            AiRetrievalClient.TriageRequest request =
                    new AiRetrievalClient.TriageRequest(
                            ticket.ticketId(),
                            ticket.subject(),
                            ticket.body(),
                            ticket.customer().tier(),
                            ticket.customer().verified());

            AiRetrievalClient.TriageResponse response;
            try {
                response = aiClient.triage(request);
            } catch (RuntimeException exception) {
                saveTrace(ticketId, "TRIAGE", List.of(), "ERROR", "FAILED");
                throw exception;
            }

            saveTrace(
                    ticketId,
                    "TRIAGE",
                    List.of(),
                    response.escalation() ? "ESCALATION_REQUIRED" : "CLEAR",
                    response.escalation() ? "ESCALATED" : "COMPLETED");
            return response;
        });
    }

    public Optional<AiRetrievalClient.DraftResponse> draft(
            String ticketId,
            int limit) {
        return ticketService.findById(ticketId).map(ticket -> {
            AiRetrievalClient.DraftRequest request =
                    new AiRetrievalClient.DraftRequest(
                            ticket.ticketId(),
                            ticket.subject(),
                            ticket.body(),
                            ticket.customer().tier(),
                            ticket.customer().verified(),
                            limit);

            AiRetrievalClient.DraftResponse response;
            try {
                response = aiClient.draft(request);
            } catch (RuntimeException exception) {
                saveTrace(ticketId, "DRAFT", List.of(), "ERROR", "FAILED");
                throw exception;
            }

            draftRepository.saveAndFlush(new DraftRecord(
                    ticketId,
                    response.status(),
                    response.draft(),
                    response.citation_ids(),
                    response.retrieved_document_ids(),
                    response.reason()));

            String guardrailResult = response.escalation()
                    ? "ESCALATION_REQUIRED"
                    : "CLEAR";
            String finalStatus = response.escalation()
                    ? "ESCALATED"
                    : response.status().toUpperCase();

            saveTrace(
                    ticketId,
                    "DRAFT",
                    response.retrieved_document_ids(),
                    guardrailResult,
                    finalStatus);
            return response;
        });
    }

    private void saveTrace(
            String ticketId,
            String runType,
            List<String> retrievedDocumentIds,
            String guardrailResult,
            String finalStatus) {
        traceRepository.saveAndFlush(new AiRunTrace(
                ticketId,
                runType,
                retrievedDocumentIds,
                List.of(),
                guardrailResult,
                finalStatus));
    }
}