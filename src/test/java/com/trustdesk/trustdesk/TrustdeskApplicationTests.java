package com.trustdesk.trustdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrustdeskApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void returnsSeededTicketWithCustomerAndOrderContext() throws Exception {
		mockMvc.perform(get("/api/tickets/tkt_9001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ticketId").value("tkt_9001"))
				.andExpect(jsonPath("$.subject").value("Received damaged earbuds"))
				.andExpect(jsonPath("$.customer.customerId").value("cus_1001"))
				.andExpect(jsonPath("$.order.orderId").value("ord_5001"));
	}


	@Test
	void returnsNotFoundForUnknownTicket() throws Exception {
		mockMvc.perform(get("/api/tickets/tkt_missing"))
				.andExpect(status().isNotFound());
	}

	@Test
	void returnsTicketList() throws Exception {
		mockMvc.perform(get("/api/tickets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].ticketId").value("tkt_9001"))
				.andExpect(jsonPath("$[0].customer.customerId").value("cus_1001"));
	}

	@Test
	void servesSupportAgentPage() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.forwardedUrl("index.html"));
	}

}