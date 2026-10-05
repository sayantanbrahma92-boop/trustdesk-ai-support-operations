package com.trustdesk.trustdesk.order;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.trustdesk.trustdesk.customer.Customer;
import com.trustdesk.trustdesk.customer.CustomerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@org.springframework.core.annotation.Order(2)
public class OrderDataLoader implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderDataLoader(
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        loadOrders();
    }

    public void loadOrders() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/orders.json");

        try (var input = resource.getInputStream()) {
            JsonNode rows = objectMapper.readTree(input);
            List<Order> orders = new ArrayList<>();

            for (JsonNode row : rows) {
                String customerId = row.path("customer_id").asText();
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Order references unknown customer: " + customerId
                        ));

                List<OrderItem> items = new ArrayList<>();
                for (JsonNode item : row.path("items")) {
                    items.add(new OrderItem(
                            item.path("sku").asText(),
                            item.path("name").asText(),
                            item.path("quantity").asInt(),
                            item.path("category").asText(),
                            item.path("final_sale").asBoolean()
                    ));
                }

                orders.add(new Order(
                        row.path("order_id").asText(),
                        customer,
                        row.path("status").asText(),
                        LocalDate.parse(row.path("placed_at").asText()),
                        parseOptionalDate(row.path("delivered_at")),
                        parseOptionalDate(row.path("eligible_return_until")),
                        row.path("total").asInt(),
                        row.path("currency").asText(),
                        row.path("payment_status").asText(),
                        nullableText(row.path("tracking_number")),
                        items
                ));
            }

            orderRepository.saveAll(orders);
        }
    }

    private LocalDate parseOptionalDate(JsonNode value) {
        return value.isNull() ? null : LocalDate.parse(value.asText());
    }

    private String nullableText(JsonNode value) {
        return value.isNull() ? null : value.asText();
    }
}