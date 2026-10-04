package com.trustdesk.trustdesk;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final TicketKnowledgeService ticketKnowledgeService;
    private final TicketAiService ticketAiService;

    public TicketController(
            TicketService ticketService,
            TicketKnowledgeService ticketKnowledgeService,
            TicketAiService ticketAiService) {
        this.ticketService = ticketService;
        this.ticketKnowledgeService = ticketKnowledgeService;
        this.ticketAiService = ticketAiService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable String id) {
        return ticketService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<TicketResponse> getTickets() {
        return ticketService.findAll();
    }

    @GetMapping("/{id}/knowledge")
    public ResponseEntity<AiRetrievalClient.RetrievalResponse> getRelevantKnowledge(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 10) {
            return ResponseEntity.badRequest().build();
        }

        return ticketKnowledgeService.findRelevantDocuments(id, limit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/triage")
    public ResponseEntity<AiRetrievalClient.TriageResponse> triageTicket(
            @PathVariable String id) {
        return ticketAiService.triage(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/draft")
    public ResponseEntity<AiRetrievalClient.DraftResponse> draftReply(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 10) {
            return ResponseEntity.badRequest().build();
        }

        return ticketAiService.draft(id, limit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}