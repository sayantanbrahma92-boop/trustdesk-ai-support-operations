package com.trustdesk.trustdesk;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private String status;
    private LocalDate placedAt;
    private LocalDate deliveredAt;
    private LocalDate eligibleReturnUntil;
    private int total;
    private String currency;

    @Column(name = "payment_status")
    private String paymentStatus;

    private String trackingNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "order_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(
            String orderId,
            Customer customer,
            String status,
            LocalDate placedAt,
            LocalDate deliveredAt,
            LocalDate eligibleReturnUntil,
            int total,
            String currency,
            String paymentStatus,
            String trackingNumber,
            List<OrderItem> items
    ) {
        this.orderId = orderId;
        this.customer = customer;
        this.status = status;
        this.placedAt = placedAt;
        this.deliveredAt = deliveredAt;
        this.eligibleReturnUntil = eligibleReturnUntil;
        this.total = total;
        this.currency = currency;
        this.paymentStatus = paymentStatus;
        this.trackingNumber = trackingNumber;
        this.items = new ArrayList<>(items);
    }

    public String getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getPlacedAt() {
        return placedAt;
    }

    public LocalDate getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDate getEligibleReturnUntil() {
        return eligibleReturnUntil;
    }

    public int getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}