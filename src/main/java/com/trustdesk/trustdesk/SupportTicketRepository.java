package com.trustdesk.trustdesk;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    List<SupportTicket> findAllByOrderByTicketIdAsc();
}