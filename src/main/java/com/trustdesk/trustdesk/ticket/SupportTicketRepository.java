package com.trustdesk.trustdesk.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    List<SupportTicket> findAllByOrderByTicketIdAsc();
}