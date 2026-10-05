package com.trustdesk.trustdesk.ticket;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TicketResponse(
        String ticketId,
        String channel,
        String subject,
        String body,
        OffsetDateTime createdAt,
        String status,
        CustomerContext customer,
        OrderContext order
) {
    public record CustomerContext(
            String customerId,
            String name,
            String email,
            String tier,
            String country,
            boolean verified
    ) {
    }

    public record OrderContext(
            String orderId,
            String status,
            LocalDate placedAt,
            LocalDate deliveredAt,
            LocalDate eligibleReturnUntil,
            int total,
            String currency,
            String paymentStatus,
            String trackingNumber,
            List<OrderItemContext> items
    ) {
    }

    public record OrderItemContext(
            String sku,
            String name,
            int quantity,
            String category,
            boolean finalSale
    ) {
    }
}