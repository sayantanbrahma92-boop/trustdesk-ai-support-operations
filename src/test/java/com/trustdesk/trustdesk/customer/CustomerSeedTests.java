package com.trustdesk.trustdesk.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trustdesk.trustdesk.customer.CustomerDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class CustomerSeedTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerDataLoader customerDataLoader;


    @Test
    void loadsAllProvidedCustomersAndCanBeRerun() throws Exception {
        customerDataLoader.loadCustomers();
        customerDataLoader.loadCustomers();

        Long customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers",
                Long.class
        );

        assertEquals(6L, customerCount);

        String customerName = jdbcTemplate.queryForObject(
                "SELECT name FROM customers WHERE customer_id = ?",
                String.class,
                "cus_1001"
        );

        assertEquals("Aisha Rao", customerName);
    }
}