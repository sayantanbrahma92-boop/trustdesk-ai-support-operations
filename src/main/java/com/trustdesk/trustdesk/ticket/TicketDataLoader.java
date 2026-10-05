package com.trustdesk.trustdesk.ticket;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.trustdesk.trustdesk.customer.Customer;
import com.trustdesk.trustdesk.customer.CustomerRepository;
import com.trustdesk.trustdesk.order.Order;
import com.trustdesk.trustdesk.order.OrderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@org.springframework.core.annotation.Order(3)
public class TicketDataLoader implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final SupportTicketRepository ticketRepository;
    private final ObjectMapper objectMapper;

    public TicketDataLoader(
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            SupportTicketRepository ticketRepository,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        loadTickets();
    }

    public void loadTickets() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/tickets.json");

        try (var input = resource.getInputStream()) {
            JsonNode rows = objectMapper.readTree(input);
            List<SupportTicket> tickets = new ArrayList<>();

            for (JsonNode row : rows) {
                String customerId = row.path("customer_id").asText();
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Ticket references unknown customer: " + customerId
                        ));

                String orderId = row.path("order_id").asText(null);
                Order order = orderId == null
                        ? null
                        : orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Ticket references unknown order: " + orderId
                        ));

                tickets.add(new SupportTicket(
                        row.path("ticket_id").asText(),
                        customer,
                        order,
                        row.path("channel").asText(),
                        row.path("subject").asText(),
                        row.path("body").asText(),
                        OffsetDateTime.parse(row.path("created_at").asText()),
                        row.path("status").asText()
                ));
            }

            ticketRepository.saveAll(tickets);
        }
    }
}