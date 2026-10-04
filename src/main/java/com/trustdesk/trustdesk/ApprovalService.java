package com.trustdesk.trustdesk;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final ApprovalActionRepository approvalRepository;
    private final SupportTicketRepository ticketRepository;
    private final RefundReviewRepository refundReviewRepository;
    private final AiRunTraceRepository traceRepository;

    public ApprovalService(
            ApprovalActionRepository approvalRepository,
            SupportTicketRepository ticketRepository,
            RefundReviewRepository refundReviewRepository,
            AiRunTraceRepository traceRepository) {
        this.approvalRepository = approvalRepository;
        this.ticketRepository = ticketRepository;
        this.refundReviewRepository = refundReviewRepository;
        this.traceRepository = traceRepository;
    }

    @Transactional
    public ApprovalAction requestRefundReview(
            String ticketId,
            String idempotencyKey) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Ticket ID is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException(
                    "Idempotency key must be between 1 and 128 characters");
        }
        if (!ticketRepository.existsById(ticketId)) {
            throw new IllegalArgumentException("Ticket does not exist");
        }

        return approvalRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    if (!existing.getTicketId().equals(ticketId)
                            || !existing.getActionName().equals(
                            ApprovalAction.ACTION_NAME)) {
                        throw new IllegalArgumentException(
                                "Idempotency key was already used for another operation");
                    }
                    return existing;
                })
                .orElseGet(() -> approvalRepository.saveAndFlush(
                        new ApprovalAction(ticketId, idempotencyKey)));
    }

    @Transactional
    public ApprovalAction approve(UUID approvalId, String reviewer) {
        ApprovalAction approval = findApproval(approvalId);

        if (approval.getStatus() == ApprovalAction.Status.REJECTED) {
            throw new IllegalStateException(
                    "A rejected request cannot be approved");
        }

        if (approval.isActionExecuted()) {
            return approval;
        }

        if (approval.getStatus() == ApprovalAction.Status.PENDING) {
            approval.approve(reviewer);
            approvalRepository.saveAndFlush(approval);
        }

        refundReviewRepository.findByApprovalId(approvalId)
                .orElseGet(() -> refundReviewRepository.saveAndFlush(
                        new RefundReview(approval)));

        approval.markActionExecuted();
        ApprovalAction executed = approvalRepository.saveAndFlush(approval);
        saveApprovalTrace(executed, "APPROVED", "EXECUTED");
        return executed;
    }

    @Transactional
    public ApprovalAction reject(UUID approvalId, String reviewer) {
        ApprovalAction approval = findApproval(approvalId);

        if (approval.getStatus() == ApprovalAction.Status.REJECTED) {
            return approval;
        }

        approval.reject(reviewer);
        ApprovalAction rejected = approvalRepository.saveAndFlush(approval);
        saveApprovalTrace(rejected, "REJECTED", "NOT_EXECUTED");
        return rejected;
    }

    private void saveApprovalTrace(
            ApprovalAction approval,
            String decision,
            String finalStatus) {
        traceRepository.saveAndFlush(new AiRunTrace(
                approval.getTicketId(),
                "APPROVAL",
                List.of(),
                List.of(approval.getActionName() + ":" + decision),
                decision,
                finalStatus));
    }

    private ApprovalAction findApproval(UUID approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Approval does not exist"));
    }
}