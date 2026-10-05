package com.trustdesk.trustdesk.knowledge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.trustdesk.trustdesk.ai.AiRetrievalClient;
import com.trustdesk.trustdesk.knowledge.TicketKnowledgeService;
import com.trustdesk.trustdesk.ticket.TicketResponse;
import com.trustdesk.trustdesk.ticket.TicketService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TicketKnowledgeServiceTest {
    @Test
    void searchesUsingTicketSubjectAndBody() {
        TicketService ticketService = mock(TicketService.class);
        AiRetrievalClient aiRetrievalClient = mock(AiRetrievalClient.class);
        TicketKnowledgeService service =
                new TicketKnowledgeService(ticketService, aiRetrievalClient);

        TicketResponse ticket = new TicketResponse(
                "tkt_9001",
                "email",
                "Received damaged earbuds",
                "My earbuds arrived damaged.",
                OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                "open",
                null,
                null);
        AiRetrievalClient.RetrievalResponse expected =
                new AiRetrievalClient.RetrievalResponse(List.of(
                        new AiRetrievalClient.RetrievedDocument(
                                "KB-REFUND-001",
                                "Eligible returns may receive a refund review.",
                                false)));

        when(ticketService.findById("tkt_9001")).thenReturn(Optional.of(ticket));
        when(aiRetrievalClient.retrieve(
                "Received damaged earbuds\nMy earbuds arrived damaged.", 5))
                .thenReturn(expected);

        AiRetrievalClient.RetrievalResponse result =
                service.findRelevantDocuments("tkt_9001", 5).orElseThrow();

        assertSame(expected, result);
        verify(aiRetrievalClient).retrieve(
                "Received damaged earbuds\nMy earbuds arrived damaged.", 5);
    }

    @Test
    void doesNotCallAiServiceWhenTicketDoesNotExist() {
        TicketService ticketService = mock(TicketService.class);
        AiRetrievalClient aiRetrievalClient = mock(AiRetrievalClient.class);
        TicketKnowledgeService service =
                new TicketKnowledgeService(ticketService, aiRetrievalClient);

        when(ticketService.findById("tkt_missing")).thenReturn(Optional.empty());

        assertEquals(
                Optional.empty(),
                service.findRelevantDocuments("tkt_missing", 5));
        verifyNoInteractions(aiRetrievalClient);
    }
}