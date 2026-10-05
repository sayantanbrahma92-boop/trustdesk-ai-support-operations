package com.trustdesk.trustdesk.customer;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(1)
public class CustomerDataLoader implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public CustomerDataLoader(
            CustomerRepository customerRepository,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        loadCustomers();
    }

    @Transactional
    public void loadCustomers() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/customers.json");

        try (var input = resource.getInputStream()) {
            JsonNode rows = objectMapper.readTree(input);
            List<Customer> customers = new ArrayList<>();

            for (JsonNode row : rows) {
                List<String> tags = new ArrayList<>();
                row.path("tags").forEach(tag -> tags.add(tag.asText()));

                customers.add(new Customer(
                        row.path("customer_id").asText(),
                        row.path("name").asText(),
                        row.path("email").asText(),
                        row.path("tier").asText(),
                        row.path("country").asText(),
                        LocalDate.parse(row.path("created_at").asText()),
                        row.path("verified").asBoolean(),
                        tags
                ));
            }

            customerRepository.saveAll(customers);
        }
    }
}