package com.careflowai.ai;

import com.careflowai.common.ConfidenceLevel;
import com.careflowai.common.UrgencyCategory;
import java.util.List;

public record AiAssessmentOutput(
    String suggestedDiagnosis,
    UrgencyCategory suggestedCategory,
    Integer suggestedScore,
    List<String> redFlagIndicators,
    List<String> missingOrAmbiguousDetails,
    String structuredSymptomSummary,
    String medicalAttentionNote,
    String staffFacingExplanation,
    ConfidenceLevel confidenceLevel
) {
    public boolean hasUsableSuggestion() {
        return suggestedScore != null || suggestedCategory != null;
    }

    public static AiAssessmentOutput unavailable() {
        return new AiAssessmentOutput(
            null,
            null,
            null,
            List.of(),
            List.of("AI advisory assessment unavailable."),
            null,
            "Use deterministic triage fallback and route to the appropriate care team.",
            "Deterministic urgency scoring was applied. AI advisory output is unavailable.",
            ConfidenceLevel.LOW
        );
    }
}
