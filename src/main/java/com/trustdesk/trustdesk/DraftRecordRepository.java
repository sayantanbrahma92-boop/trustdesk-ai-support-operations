package com.trustdesk.trustdesk;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftRecordRepository
        extends JpaRepository<DraftRecord, UUID> {
    List<DraftRecord> findAllByTicketIdOrderByCreatedAtDesc(String ticketId);
}