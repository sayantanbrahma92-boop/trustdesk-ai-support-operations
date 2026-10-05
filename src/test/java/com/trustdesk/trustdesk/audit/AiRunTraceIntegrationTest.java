package com.trustdesk.trustdesk.audit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.trustdesk.trustdesk.ai.AiRetrievalClient;
import com.trustdesk.trustdesk.ai.TicketAiService;
import com.trustdesk.trustdesk.audit.AiRunTrace;
import com.trustdesk.trustdesk.audit.AiRunTraceRepository;
import com.trustdesk.trustdesk.draft.DraftRecord;
import com.trustdesk.trustdesk.draft.DraftRecordRepository;
import com.trustdesk.trustdesk.ticket.TicketResponse;
import com.trustdesk.trustdesk.ticket.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AiRunTraceIntegrationTest {
    @Autowired
    private AiRunTraceRepository traceRepository;

    @Autowired
    private DraftRecordRepository draftRepository;

    @Test
    void persistsDraftTraceWithRetrievedIdsAndEscalationOutcome() {
        TicketService ticketService = mock(TicketService.class);
        AiRetrievalClient aiClient = mock(AiRetrievalClient.class);
        TicketAiService service =
                new TicketAiService(ticketService, aiClient, traceRepository, draftRepository);

        TicketResponse ticket = ticket();
        when(ticketService.findById("tkt_trace_draft"))
                .thenReturn(Optional.of(ticket));
        when(aiClient.draft(
                org.mockito.ArgumentMatchers.any(
                        AiRetrievalClient.DraftRequest.class)))
                .thenReturn(new AiRetrievalClient.DraftResponse(
                        "escalate",
                        null,
                        List.of(),
                        List.of("KB-REFUND-001", "KB-ADVERSARIAL-001"),
                        true,
                        "Policy support was insufficient."));

        service.draft("tkt_trace_draft", 5).orElseThrow();

        DraftRecord draft = draftRepository
                .findAllByTicketIdOrderByCreatedAtDesc("tkt_trace_draft")
                .getFirst();

        assertEquals("escalate", draft.getStatus());
        assertEquals(
                List.of("KB-REFUND-001", "KB-ADVERSARIAL-001"),
                draft.getRetrievedDocumentIds());
        assertEquals("Policy support was insufficient.", draft.getReason());

        AiRunTrace trace = traceRepository
                .findAllByTicketIdOrderByCreatedAtDesc("tkt_trace_draft")
                .getFirst();

        assertEquals("DRAFT", trace.getRunType());
        assertEquals(
                List.of("KB-REFUND-001", "KB-ADVERSARIAL-001"),
                trace.getRetrievedDocumentIds());
        assertEquals(List.of(), trace.getToolActions());
        assertEquals("ESCALATION_REQUIRED", trace.getGuardrailResult());
        assertEquals("ESCALATED", trace.getFinalStatus());
    }

    @Test
    void persistsTriageTrace() {
        TicketService ticketService = mock(TicketService.class);
        AiRetrievalClient aiClient = mock(AiRetrievalClient.class);
        TicketAiService service =
                new TicketAiService(
                        ticketService,
                        aiClient,
                        traceRepository,
                        draftRepository);

        when(ticketService.findById("tkt_trace_triage"))
                .thenReturn(Optional.of(ticket("tkt_trace_triage")));
        when(aiClient.triage(
                org.mockito.ArgumentMatchers.any(
                        AiRetrievalClient.TriageRequest.class)))
                .thenReturn(new AiRetrievalClient.TriageResponse(
                        "refund", "high", false));

        service.triage("tkt_trace_triage").orElseThrow();

        AiRunTrace trace = traceRepository
                .findAllByTicketIdOrderByCreatedAtDesc("tkt_trace_triage")
                .getFirst();

        assertEquals("TRIAGE", trace.getRunType());
        assertEquals(List.of(), trace.getRetrievedDocumentIds());
        assertEquals("CLEAR", trace.getGuardrailResult());
        assertEquals("COMPLETED", trace.getFinalStatus());
    }

    private TicketResponse ticket() {
        return ticket("tkt_trace_draft");
    }

    private TicketResponse ticket(String ticketId) {
        return new TicketResponse(
                ticketId,
                "email",
                "Received damaged earbuds",
                "My earbuds arrived damaged.",
                OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                "open",
                new TicketResponse.CustomerContext(
                        "cus_1001",
                        "Customer",
                        "customer@example.com",
                        "gold",
                        "US",
                        true),
                null);
    }
}