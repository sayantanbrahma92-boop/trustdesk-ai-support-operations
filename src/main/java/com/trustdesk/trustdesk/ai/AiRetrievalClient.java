package com.trustdesk.trustdesk.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class AiRetrievalClient {
    private final RestClient restClient;

    private final Cache<TriageRequest, TriageResponse> triageCache =
            Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .build();

    private final Cache<DraftRequest, DraftResponse> draftCache =
            Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .build();

    public AiRetrievalClient(
            RestClient.Builder builder,
            @Value("${trustdesk.ai-service.base-url:http://localhost:8001}")
            String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public RetrievalResponse retrieve(String query, int limit) {
        return restClient.post()
                .uri("/ai/retrieve")
                .body(new RetrievalRequest(query, limit))
                .retrieve()
                .body(RetrievalResponse.class);
    }

    public record RetrievalRequest(String query, int limit) {}

    public record RetrievalResponse(List<RetrievedDocument> documents) {}

    public record RetrievedDocument(
            String document_id,
            String text,
            boolean is_untrusted) {}


    public TriageResponse triage(TriageRequest request) {
        return triageCache.get(request, key -> restClient.post()
                .uri("/ai/triage")
                .body(key)
                .retrieve()
                .body(TriageResponse.class));
    }

    public DraftResponse draft(DraftRequest request) {
        return draftCache.get(request, key -> restClient.post()
                .uri("/ai/draft")
                .body(key)
                .retrieve()
                .body(DraftResponse.class));
    }

    public record TriageRequest(
            String ticket_id,
            String subject,
            String body,
            String customer_tier,
            boolean customer_verified) {}

    public record TriageResponse(
            String category,
            String priority,
            boolean escalation) {}

    public record DraftRequest(
            String ticket_id,
            String subject,
            String body,
            String customer_tier,
            boolean customer_verified,
            int limit) {}

    public record DraftResponse(
            String status,
            String draft,
            List<String> citation_ids,
            List<String> retrieved_document_ids,
            boolean escalation,
            String reason) {}

}