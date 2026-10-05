package com.trustdesk.trustdesk.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trustdesk.trustdesk.ticket.TicketDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TicketSeedTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TicketDataLoader ticketDataLoader;

    @Test
    void loadsAllProvidedTicketsAndCanBeRerun() throws Exception {
        ticketDataLoader.loadTickets();
        ticketDataLoader.loadTickets();

        Long ticketCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tickets",
                Long.class
        );
        assertEquals(8L, ticketCount);

        String customerId = jdbcTemplate.queryForObject(
                "SELECT customer_id FROM tickets WHERE ticket_id = ?",
                String.class,
                "tkt_9001"
        );
        assertEquals("cus_1001", customerId);

        String orderId = jdbcTemplate.queryForObject(
                "SELECT order_id FROM tickets WHERE ticket_id = ?",
                String.class,
                "tkt_9001"
        );
        assertEquals("ord_5001", orderId);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM tickets WHERE ticket_id = ?",
                String.class,
                "tkt_9001"
        );
        assertEquals("open", status);
    }
}