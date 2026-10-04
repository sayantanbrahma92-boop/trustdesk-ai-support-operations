package com.trustdesk.trustdesk;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRunTraceRepository extends JpaRepository<AiRunTrace, UUID> {
    List<AiRunTrace> findAllByTicketIdOrderByCreatedAtDesc(String ticketId);
}