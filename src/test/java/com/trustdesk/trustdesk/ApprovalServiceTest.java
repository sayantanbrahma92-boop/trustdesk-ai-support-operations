package com.trustdesk.trustdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ApprovalServiceTest {
    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalActionRepository approvalRepository;

    @Autowired
    private RefundReviewRepository refundReviewRepository;

    @Autowired
    private AiRunTraceRepository traceRepository;

    @Test
    void requestCreatesPendingApprovalWithoutExecutingAction() {
        ApprovalAction approval = approvalService.requestRefundReview(
                "tkt_9001",
                "refund-review-pending-test");

        ApprovalAction saved =
                approvalRepository.findById(approval.getId()).orElseThrow();

        assertEquals(ApprovalAction.Status.PENDING, saved.getStatus());
        assertEquals(ApprovalAction.ACTION_NAME, saved.getActionName());
        assertFalse(saved.isActionExecuted());
        assertEquals(
                0,
                refundReviewRepository.countByApprovalId(approval.getId()));
    }

    @Test
    void sameIdempotencyKeyReturnsExistingApproval() {
        ApprovalAction first = approvalService.requestRefundReview(
                "tkt_9001",
                "refund-review-retry-test");

        ApprovalAction retry = approvalService.requestRefundReview(
                "tkt_9001",
                "refund-review-retry-test");

        assertEquals(first.getId(), retry.getId());
        assertEquals(
                1,
                approvalRepository.countByIdempotencyKey(
                        "refund-review-retry-test"));
    }

    @Test
    void conflictingIdempotencyKeyReuseIsRejected() {
        approvalService.requestRefundReview(
                "tkt_9001",
                "refund-review-conflict-test");

        assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.requestRefundReview(
                        "tkt_9002",
                        "refund-review-conflict-test"));
    }

    @Test
    void rejectionDoesNotExecuteActionAndCannotLaterBeApproved() {
        ApprovalAction pending = approvalService.requestRefundReview(
                "tkt_9001",
                "refund-review-reject-test");

        ApprovalAction rejected =
                approvalService.reject(pending.getId(), "agent-1");

        assertEquals(ApprovalAction.Status.REJECTED, rejected.getStatus());
        assertFalse(rejected.isActionExecuted());
        assertThrows(
                IllegalStateException.class,
                () -> approvalService.approve(pending.getId(), "agent-2"));

        AiRunTrace trace = traceRepository
                .findAllByTicketIdOrderByCreatedAtDesc("tkt_9001")
                .stream()
                .filter(item -> item.getRunType().equals("APPROVAL"))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of("start_refund_review:REJECTED"), trace.getToolActions());
        assertEquals("REJECTED", trace.getGuardrailResult());
        assertEquals("NOT_EXECUTED", trace.getFinalStatus());
    }

    @Test
    void approvalRecordsExecutionOnceAndRetryReturnsSameResult() {
        ApprovalAction pending = approvalService.requestRefundReview(
                "tkt_9001",
                "refund-review-approve-test");

        ApprovalAction approved =
                approvalService.approve(pending.getId(), "agent-1");
        ApprovalAction retry =
                approvalService.approve(pending.getId(), "agent-2");

        assertEquals(ApprovalAction.Status.APPROVED, approved.getStatus());
        assertTrue(approved.isActionExecuted());
        assertEquals(approved.getId(), retry.getId());
        assertTrue(retry.isActionExecuted());
        assertNotEquals(null, approved.getReviewedAt());
        assertEquals(
                1,
                refundReviewRepository.countByApprovalId(pending.getId()));

        AiRunTrace trace = traceRepository
                .findAllByTicketIdOrderByCreatedAtDesc("tkt_9001")
                .stream()
                .filter(item -> item.getRunType().equals("APPROVAL"))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of("start_refund_review:APPROVED"), trace.getToolActions());
        assertEquals("APPROVED", trace.getGuardrailResult());
        assertEquals("EXECUTED", trace.getFinalStatus());
    }
}