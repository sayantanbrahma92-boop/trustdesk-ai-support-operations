package com.trustdesk.trustdesk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_run_traces")
public class AiRunTrace {
    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(name = "run_type", nullable = false)
    private String runType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ai_run_trace_documents",
            joinColumns = @JoinColumn(name = "trace_id"))
    @Column(name = "document_id", nullable = false)
    @OrderColumn(name = "position")
    private List<String> retrievedDocumentIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ai_run_trace_tool_actions",
            joinColumns = @JoinColumn(name = "trace_id"))
    @Column(name = "action_name", nullable = false)
    @OrderColumn(name = "position")
    private List<String> toolActions = new ArrayList<>();

    @Column(name = "guardrail_result", nullable = false)
    private String guardrailResult;

    @Column(name = "final_status", nullable = false)
    private String finalStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiRunTrace() {
    }

    public AiRunTrace(
            String ticketId,
            String runType,
            List<String> retrievedDocumentIds,
            List<String> toolActions,
            String guardrailResult,
            String finalStatus) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.runType = runType;
        this.retrievedDocumentIds = new ArrayList<>(retrievedDocumentIds);
        this.toolActions = new ArrayList<>(toolActions);
        this.guardrailResult = guardrailResult;
        this.finalStatus = finalStatus;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getRunType() {
        return runType;
    }

    public List<String> getRetrievedDocumentIds() {
        return List.copyOf(retrievedDocumentIds);
    }

    public List<String> getToolActions() {
        return List.copyOf(toolActions);
    }

    public String getGuardrailResult() {
        return guardrailResult;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}