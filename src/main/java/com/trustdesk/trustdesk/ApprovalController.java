package com.trustdesk.trustdesk;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ApprovalController {
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/api/tickets/{ticketId}/refund-review")
    public ResponseEntity<ApprovalResponse> requestRefundReview(
            @PathVariable String ticketId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(toResponse(
                approvalService.requestRefundReview(
                        ticketId,
                        idempotencyKey)));
    }

    @PostMapping("/api/approvals/{approvalId}/approve")
    public ResponseEntity<ApprovalResponse> approve(
            @PathVariable UUID approvalId,
            @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(toResponse(
                approvalService.approve(approvalId, request.reviewer())));
    }

    @PostMapping("/api/approvals/{approvalId}/reject")
    public ResponseEntity<ApprovalResponse> reject(
            @PathVariable UUID approvalId,
            @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(toResponse(
                approvalService.reject(approvalId, request.reviewer())));
    }

    private ApprovalResponse toResponse(ApprovalAction action) {
        return new ApprovalResponse(
                action.getId(),
                action.getTicketId(),
                action.getActionName(),
                action.getIdempotencyKey(),
                action.getStatus().name(),
                action.isActionExecuted(),
                action.getRequestedAt(),
                action.getReviewedAt(),
                action.getReviewedBy());
    }

    public record ReviewRequest(String reviewer) {
    }

    public record ApprovalResponse(
            UUID id,
            String ticketId,
            String actionName,
            String idempotencyKey,
            String status,
            boolean actionExecuted,
            Instant requestedAt,
            Instant reviewedAt,
            String reviewedBy) {
    }
}