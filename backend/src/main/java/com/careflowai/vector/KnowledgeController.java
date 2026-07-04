package com.careflowai.vector;

import com.careflowai.vector.dto.KnowledgeDocumentResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final SimpleIntakeVectorStore vectorStore;

    public KnowledgeController(SimpleIntakeVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping
    public List<KnowledgeDocumentResponse> list() {
        return vectorStore.listKnowledge().stream()
            .map(KnowledgeDocumentResponse::from)
            .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeDocumentResponse upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String title) throws IOException {
        String fileName = StringUtils.hasText(file.getOriginalFilename())
            ? file.getOriginalFilename()
            : "hospital-knowledge.txt";
        String content = extractReadableText(file.getBytes());
        String safeTitle = StringUtils.hasText(title) ? title.trim() : fileName;
        return KnowledgeDocumentResponse.from(vectorStore.indexKnowledge(safeTitle, fileName, content));
    }

    private String extractReadableText(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        String normalized = raw
            .replaceAll("\\\\r|\\\\n", " ")
            .replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.length() > 200_000) {
            return normalized.substring(0, 200_000);
        }
        return normalized;
    }
}
