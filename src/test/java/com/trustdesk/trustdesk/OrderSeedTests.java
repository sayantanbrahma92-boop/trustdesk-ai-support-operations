package com.trustdesk.trustdesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class OrderSeedTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderDataLoader orderDataLoader;

    @Test
    void loadsProvidedOrdersWithCustomerLinksAndCanBeRerun() throws Exception {
        orderDataLoader.loadOrders();
        orderDataLoader.loadOrders();

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders",
                Long.class
        );
        assertEquals(6L, count);

        String customerId = jdbcTemplate.queryForObject(
                "SELECT customer_id FROM orders WHERE order_id = ?",
                String.class,
                "ord_5001"
        );
        assertEquals("cus_1001", customerId);

        Boolean deliveryDateIsNull = jdbcTemplate.queryForObject(
                "SELECT delivered_at IS NULL FROM orders WHERE order_id = ?",
                Boolean.class,
                "ord_5002"
        );
        assertTrue(deliveryDateIsNull);
    }
}