package com.trustdesk.trustdesk.approval;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundReviewRepository extends JpaRepository<RefundReview, UUID> {
    Optional<RefundReview> findByApprovalId(UUID approvalId);

    long countByApprovalId(UUID approvalId);
}