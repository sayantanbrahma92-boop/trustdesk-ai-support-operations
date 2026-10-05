package com.trustdesk.trustdesk.approval;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "refund_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refund_review_approval",
                        columnNames = "approval_id"),
                @UniqueConstraint(
                        name = "uk_refund_review_idempotency",
                        columnNames = "idempotency_key")
        })
public class RefundReview {
    @Id
    private UUID id;

    @Column(name = "approval_id", nullable = false)
    private UUID approvalId;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefundReview() {
    }

    public RefundReview(ApprovalAction approval) {
        id = UUID.randomUUID();
        approvalId = approval.getId();
        ticketId = approval.getTicketId();
        idempotencyKey = approval.getIdempotencyKey();
        status = "STARTED";
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}