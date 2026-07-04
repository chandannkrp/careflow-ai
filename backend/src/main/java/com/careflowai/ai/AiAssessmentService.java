package com.careflowai.ai;

import com.careflowai.common.ConfidenceLevel;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.intake.Intake;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiAssessmentService {

    private static final String INSTRUCTION = """
        You are a hospital triage support assistant for intake routing.
        Return only compact JSON with no markdown and these fields:
        suggestedDiagnosis, suggestedCategory, suggestedScore, redFlagIndicators,
        missingOrAmbiguousDetails, structuredSymptomSummary, medicalAttentionNote,
        staffFacingExplanation, confidenceLevel.
        suggestedDiagnosis is a brief likely clinical concern or differential label, not a final diagnosis.
        suggestedCategory is the urgency level and must be one of CRITICAL, HIGH, MEDIUM, LOW.
        suggestedScore is a 0-100 queue severity score consistent with the category.
        medicalAttentionNote is one short staff note about the attention needed next.
        Do not prescribe medications or treatment plans. Confidence is LOW, MEDIUM, HIGH.
        """;

    private final OpenAiResponsesClient responsesClient;
    private final SpringAiChatService springAiChatService;
    private final ObjectMapper objectMapper;

    public AiAssessmentService(OpenAiResponsesClient responsesClient,
                               SpringAiChatService springAiChatService,
                               ObjectMapper objectMapper) {
        this.responsesClient = responsesClient;
        this.springAiChatService = springAiChatService;
        this.objectMapper = objectMapper;
    }

    public AiAssessmentOutput assess(Intake intake) {
        try {
            return parseResponse(springAiChatService.respond(INSTRUCTION, objectMapper.writeValueAsString(payload(intake))));
        } catch (Exception springAiFailure) {
            if (!responsesClient.isAvailable()) {
                return AiAssessmentOutput.unavailable();
            }
            try {
                return parseResponse(responsesClient.respond(INSTRUCTION, objectMapper.writeValueAsString(payload(intake))));
            } catch (Exception ignored) {
                return AiAssessmentOutput.unavailable();
            }
        }
    }

    private AiAssessmentOutput parseResponse(String response) throws java.io.IOException {
        JsonNode json = objectMapper.readTree(extractJson(response));
        return new AiAssessmentOutput(
            cleanText(json.path("suggestedDiagnosis").asText(null)),
            parseCategory(json.path("suggestedCategory").asText(null)),
            json.path("suggestedScore").isNumber() ? json.path("suggestedScore").asInt() : null,
            stringList(json.path("redFlagIndicators")),
            stringList(json.path("missingOrAmbiguousDetails")),
            cleanText(json.path("structuredSymptomSummary").asText(null)),
            cleanText(json.path("medicalAttentionNote").asText(null)),
            cleanText(json.path("staffFacingExplanation").asText(null)),
            parseConfidence(json.path("confidenceLevel").asText(null))
        );
    }

    private Map<String, Object> payload(Intake intake) {
        return Map.of(
            "ageBand", intake.getPatient().getAgeBand(),
            "arrivalMode", intake.getArrivalMode(),
            "chiefComplaint", intake.getChiefComplaint(),
            "symptomNotes", intake.getSymptomNotes() == null ? "" : intake.getSymptomNotes(),
            "structuredSymptoms", intake.getStructuredSymptoms(),
            "clinicalDistressScore", intake.getPainLevel(),
            "vitals", intake.getVitals(),
            "riskFlags", intake.getRiskFlags(),
            "department", intake.getDepartment(),
            "status", intake.getCurrentStatus()
        );
    }

    private String extractJson(String response) {
        int first = response.indexOf('{');
        int last = response.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return response.substring(first, last + 1);
        }
        return response;
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(value -> {
                if (value.isTextual() && !value.asText().isBlank()) {
                    values.add(value.asText());
                }
            });
        }
        return values;
    }

    private UrgencyCategory parseCategory(String value) {
        try {
            return value == null ? null : UrgencyCategory.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ConfidenceLevel parseConfidence(String value) {
        try {
            return value == null ? ConfidenceLevel.LOW : ConfidenceLevel.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ConfidenceLevel.LOW;
        }
    }
}
