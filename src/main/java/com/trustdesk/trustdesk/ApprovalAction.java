package com.trustdesk.trustdesk;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "approval_actions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_approval_actions_idempotency_key",
                columnNames = "idempotency_key"
        )
)
public class ApprovalAction {
    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public static final String ACTION_NAME = "start_refund_review";

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Column(name = "action_name", nullable = false)
    private String actionName;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private Status status;

    @Column(name = "action_executed", nullable = false)
    private boolean actionExecuted;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Version
    private long version;

    protected ApprovalAction() {
    }

    public ApprovalAction(String ticketId, String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.actionName = ACTION_NAME;
        this.idempotencyKey = idempotencyKey;
        this.status = Status.PENDING;
        this.actionExecuted = false;
        this.requestedAt = Instant.now();
    }

    public void approve(String reviewer) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved");
        }
        if (reviewer == null || reviewer.isBlank()) {
            throw new IllegalArgumentException("Reviewer is required");
        }

        status = Status.APPROVED;
        reviewedBy = reviewer;
        reviewedAt = Instant.now();
    }

    public void markActionExecuted() {
        if (status != Status.APPROVED) {
            throw new IllegalStateException(
                    "Action can only execute after approval");
        }
        actionExecuted = true;
    }

    public void reject(String reviewer) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }
        if (reviewer == null || reviewer.isBlank()) {
            throw new IllegalArgumentException("Reviewer is required");
        }

        status = Status.REJECTED;
        reviewedBy = reviewer;
        reviewedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getActionName() {
        return actionName;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isActionExecuted() {
        return actionExecuted;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }
}