package com.careflowai.vector.dto;

import com.careflowai.vector.KnowledgeDocument;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentResponse(
    UUID id,
    String title,
    String fileName,
    int contentLength,
    Instant updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
            document.getId(),
            document.getTitle(),
            document.getFileName(),
            document.getContent().length(),
            document.getUpdatedAt()
        );
    }
}
