package com.trustdesk.trustdesk.ticket;

import java.util.List;
import java.util.Optional;

import com.trustdesk.trustdesk.customer.Customer;
import com.trustdesk.trustdesk.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final SupportTicketRepository ticketRepository;

    public TicketService(SupportTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        return ticketRepository.findAllByOrderByTicketIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TicketResponse> findById(String id) {
        return ticketRepository.findById(id)
                .map(this::toResponse);
    }

    private TicketResponse toResponse(SupportTicket ticket) {
        Customer customer = ticket.getCustomer();

        TicketResponse.CustomerContext customerContext =
                new TicketResponse.CustomerContext(
                        customer.getCustomerId(),
                        customer.getName(),
                        customer.getEmail(),
                        customer.getTier(),
                        customer.getCountry(),
                        customer.isVerified()
                );

        Order order = ticket.getOrder();
        TicketResponse.OrderContext orderContext = order == null
                ? null
                : new TicketResponse.OrderContext(
                order.getOrderId(),
                order.getStatus(),
                order.getPlacedAt(),
                order.getDeliveredAt(),
                order.getEligibleReturnUntil(),
                order.getTotal(),
                order.getCurrency(),
                order.getPaymentStatus(),
                order.getTrackingNumber(),
                order.getItems().stream()
                        .map(item -> new TicketResponse.OrderItemContext(
                                item.getSku(),
                                item.getName(),
                                item.getQuantity(),
                                item.getCategory(),
                                item.isFinalSale()
                        ))
                        .toList()
        );

        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getChannel(),
                ticket.getSubject(),
                ticket.getBody(),
                ticket.getCreatedAt(),
                ticket.getStatus(),
                customerContext,
                orderContext
        );
    }
}