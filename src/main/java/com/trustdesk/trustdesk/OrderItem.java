package com.trustdesk.trustdesk;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderItem {

    private String sku;
    private String name;
    private int quantity;
    private String category;

    @Column(name = "final_sale")
    private boolean finalSale;

    protected OrderItem() {
    }

    public OrderItem(
            String sku,
            String name,
            int quantity,
            String category,
            boolean finalSale
    ) {
        this.sku = sku;
        this.name = name;
        this.quantity = quantity;
        this.category = category;
        this.finalSale = finalSale;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCategory() {
        return category;
    }

    public boolean isFinalSale() {
        return finalSale;
    }
}