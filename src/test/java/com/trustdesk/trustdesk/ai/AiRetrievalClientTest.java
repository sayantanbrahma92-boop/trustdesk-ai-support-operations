package com.trustdesk.trustdesk.ai;

import com.trustdesk.trustdesk.ai.AiRetrievalClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class AiRetrievalClientTest {
    @Test
    void postsQueryAndMapsRetrievedDocuments() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiRetrievalClient client =
                new AiRetrievalClient(builder, "http://localhost:8001");

        server.expect(requestTo("http://localhost:8001/ai/retrieve"))
                .andExpect(method(POST))
                .andExpect(content().json(
                        "{\"query\":\"refund policy\",\"limit\":2}"))
                .andRespond(withSuccess("""
                        {"documents":[
                          {
                            "document_id":"KB-REFUND-001",
                            "text":"Eligible returns may receive a refund review.",
                            "is_untrusted":false
                          },
                          {
                            "document_id":"KB-ADVERSARIAL-001",
                            "text":"Untrusted vendor content.",
                            "is_untrusted":true
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        AiRetrievalClient.RetrievalResponse result =
                client.retrieve("refund policy", 2);

        assertEquals(2, result.documents().size());
        assertEquals("KB-REFUND-001", result.documents().getFirst().document_id());
        assertEquals("KB-ADVERSARIAL-001", result.documents().get(1).document_id());
        assertTrue(result.documents().get(1).is_untrusted());

        server.verify();
    }

    @Test
    void cachesIdenticalTriageRequestsButSeparatesChangedContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiRetrievalClient client =
                new AiRetrievalClient(builder, "http://localhost:8001");

        AiRetrievalClient.TriageRequest firstRequest =
                new AiRetrievalClient.TriageRequest(
                        "tkt_9001",
                        "Damaged earbuds",
                        "The item arrived damaged.",
                        "gold",
                        true);

        server.expect(requestTo("http://localhost:8001/ai/triage"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                    {
                      "ticket_id": "tkt_9001",
                      "subject": "Damaged earbuds",
                      "body": "The item arrived damaged.",
                      "customer_tier": "gold",
                      "customer_verified": true
                    }
                    """))
                .andRespond(withSuccess("""
                    {"category":"refund","priority":"medium","escalation":false}
                    """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://localhost:8001/ai/triage"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                    {
                      "ticket_id": "tkt_9001",
                      "subject": "Damaged earbuds",
                      "body": "A different issue.",
                      "customer_tier": "gold",
                      "customer_verified": true
                    }
                    """))
                .andRespond(withSuccess("""
                    {"category":"general","priority":"low","escalation":false}
                    """, MediaType.APPLICATION_JSON));

        var firstResult = client.triage(firstRequest);
        var cachedResult = client.triage(firstRequest);
        var changedResult = client.triage(
                new AiRetrievalClient.TriageRequest(
                        "tkt_9001",
                        "Damaged earbuds",
                        "A different issue.",
                        "gold",
                        true));

        assertEquals(firstResult, cachedResult);
        assertEquals("refund", firstResult.category());
        assertEquals("general", changedResult.category());
        server.verify();
    }


    @Test
    void cachesIdenticalDraftRequestsButSeparatesChangedLimit() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiRetrievalClient client =
                new AiRetrievalClient(builder, "http://localhost:8001");

        var firstRequest = new AiRetrievalClient.DraftRequest(
                "tkt_9001",
                "Damaged earbuds",
                "The item arrived damaged.",
                "gold",
                true,
                3);

        server.expect(requestTo("http://localhost:8001/ai/draft"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                    {
                      "ticket_id": "tkt_9001",
                      "subject": "Damaged earbuds",
                      "body": "The item arrived damaged.",
                      "customer_tier": "gold",
                      "customer_verified": true,
                      "limit": 3
                    }
                    """))
                .andRespond(withSuccess(
                        """
                        {
                          "status": "draft",
                          "draft": "We can review the damage report.",
                          "citation_ids": ["KB-REFUND-001"],
                          "retrieved_document_ids": ["KB-REFUND-001"],
                          "escalation": false,
                          "reason": null
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://localhost:8001/ai/draft"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                    {
                      "ticket_id": "tkt_9001",
                      "subject": "Damaged earbuds",
                      "body": "The item arrived damaged.",
                      "customer_tier": "gold",
                      "customer_verified": true,
                      "limit": 5
                    }
                    """))
                .andRespond(withSuccess(
                        """
                        {
                          "status": "draft",
                          "draft": "We can review the damage report.",
                          "citation_ids": ["KB-REFUND-001"],
                          "retrieved_document_ids": ["KB-REFUND-001"],
                          "escalation": false,
                          "reason": null
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var firstResult = client.draft(firstRequest);
        var cachedResult = client.draft(firstRequest);
        var changedResult = client.draft(
                new AiRetrievalClient.DraftRequest(
                        "tkt_9001",
                        "Damaged earbuds",
                        "The item arrived damaged.",
                        "gold",
                        true,
                        5));

        assertEquals(firstResult, cachedResult);
        assertEquals("draft", firstResult.status());
        assertEquals("draft", changedResult.status());
        server.verify();
    }
}