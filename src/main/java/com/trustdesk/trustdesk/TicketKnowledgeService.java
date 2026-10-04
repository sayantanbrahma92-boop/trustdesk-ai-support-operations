package com.trustdesk.trustdesk;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class TicketKnowledgeService {
    private final TicketService ticketService;
    private final AiRetrievalClient aiRetrievalClient;

    public TicketKnowledgeService(
            TicketService ticketService,
            AiRetrievalClient aiRetrievalClient) {
        this.ticketService = ticketService;
        this.aiRetrievalClient = aiRetrievalClient;
    }

    public Optional<AiRetrievalClient.RetrievalResponse> findRelevantDocuments(
            String ticketId,
            int limit) {
        return ticketService.findById(ticketId)
                .map(ticket -> {
                    String query = ticket.subject() + "\n" + ticket.body();
                    return aiRetrievalClient.retrieve(query, limit);
                });
    }
}