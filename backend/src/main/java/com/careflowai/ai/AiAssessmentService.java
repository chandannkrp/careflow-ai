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
        You are an administrative triage support assistant for a hospital queue MVP.
        You do not diagnose or recommend treatment. Return only compact JSON with:
        suggestedCategory, suggestedScore, redFlagIndicators, missingOrAmbiguousDetails,
        structuredSymptomSummary, staffFacingExplanation, confidenceLevel.
        Categories are CRITICAL, HIGH, MEDIUM, LOW. Confidence is LOW, MEDIUM, HIGH.
        """;

    private final OpenAiResponsesClient responsesClient;
    private final ObjectMapper objectMapper;

    public AiAssessmentService(OpenAiResponsesClient responsesClient, ObjectMapper objectMapper) {
        this.responsesClient = responsesClient;
        this.objectMapper = objectMapper;
    }

    public AiAssessmentOutput assess(Intake intake) {
        if (!responsesClient.isAvailable()) {
            return AiAssessmentOutput.unavailable();
        }

        try {
            String response = responsesClient.respond(INSTRUCTION, objectMapper.writeValueAsString(payload(intake)));
            JsonNode json = objectMapper.readTree(extractJson(response));
            return new AiAssessmentOutput(
                parseCategory(json.path("suggestedCategory").asText(null)),
                json.path("suggestedScore").isNumber() ? json.path("suggestedScore").asInt() : null,
                stringList(json.path("redFlagIndicators")),
                stringList(json.path("missingOrAmbiguousDetails")),
                json.path("structuredSymptomSummary").asText(null),
                json.path("staffFacingExplanation").asText(null),
                parseConfidence(json.path("confidenceLevel").asText(null))
            );
        } catch (Exception ignored) {
            return AiAssessmentOutput.unavailable();
        }
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
