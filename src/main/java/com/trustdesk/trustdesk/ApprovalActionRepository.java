package com.trustdesk.trustdesk;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;




public interface ApprovalActionRepository
        extends JpaRepository<ApprovalAction, UUID> {
    Optional<ApprovalAction> findByIdempotencyKey(String idempotencyKey);
    long countByIdempotencyKey(String idempotencyKey);
}