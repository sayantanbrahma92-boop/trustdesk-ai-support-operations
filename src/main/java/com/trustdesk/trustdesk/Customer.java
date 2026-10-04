package com.trustdesk.trustdesk;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    private String name;
    private String email;
    private String tier;
    private String country;
    private LocalDate createdAt;
    private boolean verified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "customer_tags",
            joinColumns = @JoinColumn(name = "customer_id")
    )
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    protected Customer() {
    }

    public Customer(
            String customerId,
            String name,
            String email,
            String tier,
            String country,
            LocalDate createdAt,
            boolean verified,
            List<String> tags
    ) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.tier = tier;
        this.country = country;
        this.createdAt = createdAt;
        this.verified = verified;
        this.tags = new ArrayList<>(tags);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTier() {
        return tier;
    }

    public String getCountry() {
        return country;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public List<String> getTags() {
        return List.copyOf(tags);
    }
}