package com.trustdesk.trustdesk.ai;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TicketAiServiceTest {
    private final TicketService ticketService = mock(TicketService.class);
    private final AiRetrievalClient aiClient = mock(AiRetrievalClient.class);
    private final AiRunTraceRepository traceRepository =
            mock(AiRunTraceRepository.class);

    private final DraftRecordRepository draftRepository =
            mock(DraftRecordRepository.class);

    private final TicketAiService service =
            new TicketAiService(
                    ticketService,
                    aiClient,
                    traceRepository,
                    draftRepository);

    private TicketResponse ticket() {
        return new TicketResponse(
                "tkt_9001",
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

    @Test
    void triagesTicketUsingCustomerContext() {
        when(ticketService.findById("tkt_9001"))
                .thenReturn(Optional.of(ticket()));
        when(aiClient.triage(new AiRetrievalClient.TriageRequest(
                "tkt_9001",
                "Received damaged earbuds",
                "My earbuds arrived damaged.",
                "gold",
                true)))
                .thenReturn(new AiRetrievalClient.TriageResponse(
                        "refund", "high", true));

        var result = service.triage("tkt_9001").orElseThrow();

        assertEquals("refund", result.category());
        assertEquals("high", result.priority());
        assertTrue(result.escalation());
        verify(traceRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.any(AiRunTrace.class));
    }

    @Test
    void draftsReplyAndForwardsRetrievalLimit() {
        when(ticketService.findById("tkt_9001"))
                .thenReturn(Optional.of(ticket()));
        when(aiClient.draft(new AiRetrievalClient.DraftRequest(
                "tkt_9001",
                "Received damaged earbuds",
                "My earbuds arrived damaged.",
                "gold",
                true,
                3)))
                .thenReturn(new AiRetrievalClient.DraftResponse(
                        "draft",
                        "We can review your request.",
                        List.of("KB-REFUND-001"),
                        List.of("KB-REFUND-001"),
                        false,
                        null));

        var result = service.draft("tkt_9001", 3).orElseThrow();

        assertEquals("draft", result.status());
        assertEquals(List.of("KB-REFUND-001"), result.citation_ids());
        assertEquals(
                List.of("KB-REFUND-001"),
                result.retrieved_document_ids());
        verify(aiClient).draft(new AiRetrievalClient.DraftRequest(
                "tkt_9001",
                "Received damaged earbuds",
                "My earbuds arrived damaged.",
                "gold",
                true,
                3));
        verify(traceRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.any(AiRunTrace.class));
        verify(draftRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.any(DraftRecord.class));
    }

    @Test
    void missingTicketDoesNotCallPythonOrSaveTrace() {
        when(ticketService.findById("tkt_missing"))
                .thenReturn(Optional.empty());

        assertTrue(service.triage("tkt_missing").isEmpty());
        assertTrue(service.draft("tkt_missing", 5).isEmpty());
        verifyNoInteractions(aiClient, traceRepository);
    }
}