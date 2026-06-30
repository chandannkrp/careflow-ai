package com.careflowai.ai;

import com.careflowai.common.ConfidenceLevel;
import com.careflowai.common.UrgencyCategory;
import java.util.List;

public record AiAssessmentOutput(
    UrgencyCategory suggestedCategory,
    Integer suggestedScore,
    List<String> redFlagIndicators,
    List<String> missingOrAmbiguousDetails,
    String structuredSymptomSummary,
    String staffFacingExplanation,
    ConfidenceLevel confidenceLevel
) {
    public static AiAssessmentOutput unavailable() {
        return new AiAssessmentOutput(
            null,
            null,
            List.of(),
            List.of("AI advisory assessment unavailable."),
            null,
            "Deterministic urgency scoring was applied. AI advisory output is unavailable.",
            ConfidenceLevel.LOW
        );
    }
}
