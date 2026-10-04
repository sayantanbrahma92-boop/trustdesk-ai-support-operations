package com.trustdesk.trustdesk;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class DraftRecordIntegrationTest {
    @Autowired
    private DraftRecordRepository draftRepository;

    @Test
    void persistsDraftTextStatusAndDocumentIds() {
        DraftRecord draft = draftRepository.saveAndFlush(new DraftRecord(
                "tkt_draft_persistence",
                "draft",
                "We can review your damaged item.",
                List.of("KB-REFUND-001"),
                List.of("KB-REFUND-001", "KB-WARRANTY-001"),
                null));

        DraftRecord saved = draftRepository
                .findAllByTicketIdOrderByCreatedAtDesc("tkt_draft_persistence")
                .getFirst();

        assertEquals(draft.getId(), saved.getId());
        assertEquals("draft", saved.getStatus());
        assertEquals("We can review your damaged item.", saved.getDraftText());
        assertEquals(List.of("KB-REFUND-001"), saved.getCitationIds());
        assertEquals(
                List.of("KB-REFUND-001", "KB-WARRANTY-001"),
                saved.getRetrievedDocumentIds());
        assertNotNull(saved.getCreatedAt());
    }
}