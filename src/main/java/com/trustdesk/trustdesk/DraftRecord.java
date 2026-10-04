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
@Table(name = "ai_drafts")
public class DraftRecord {
    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String status;

    @Column(name = "draft_text", columnDefinition = "text")
    private String draftText;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ai_draft_citations",
            joinColumns = @JoinColumn(name = "draft_id"))
    @Column(name = "document_id", nullable = false)
    @OrderColumn(name = "position")
    private List<String> citationIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ai_draft_retrieved_documents",
            joinColumns = @JoinColumn(name = "draft_id"))
    @Column(name = "document_id", nullable = false)
    @OrderColumn(name = "position")
    private List<String> retrievedDocumentIds = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DraftRecord() {
    }

    public DraftRecord(
            String ticketId,
            String status,
            String draftText,
            List<String> citationIds,
            List<String> retrievedDocumentIds,
            String reason) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.status = status;
        this.draftText = draftText;
        this.citationIds = new ArrayList<>(citationIds);
        this.retrievedDocumentIds = new ArrayList<>(retrievedDocumentIds);
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getStatus() {
        return status;
    }

    public String getDraftText() {
        return draftText;
    }

    public List<String> getCitationIds() {
        return List.copyOf(citationIds);
    }

    public List<String> getRetrievedDocumentIds() {
        return List.copyOf(retrievedDocumentIds);
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}