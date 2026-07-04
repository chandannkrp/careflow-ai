package com.careflowai.assessment.dto;

import com.careflowai.assessment.UrgencyAssessment;
import com.careflowai.common.ConfidenceLevel;
import com.careflowai.common.UrgencyCategory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record UrgencyAssessmentResponse(
    UUID id,
    UrgencyCategory finalCategory,
    int finalScore,
    List<String> scoreFactors,
    UrgencyCategory suggestedCategory,
    Integer suggestedScore,
    List<String> redFlagIndicators,
    List<String> missingOrAmbiguousDetails,
    String structuredSymptomSummary,
    String suggestedDiagnosis,
    String medicalAttentionNote,
    String staffFacingExplanation,
    ConfidenceLevel confidenceLevel,
    Instant assessedAt
) {
    public static UrgencyAssessmentResponse from(UrgencyAssessment assessment) {
        return new UrgencyAssessmentResponse(
            assessment.getId(),
            assessment.getFinalCategory(),
            assessment.getFinalScore(),
            new ArrayList<>(assessment.getScoreFactors()),
            assessment.getSuggestedCategory(),
            assessment.getSuggestedScore(),
            new ArrayList<>(assessment.getRedFlagIndicators()),
            new ArrayList<>(assessment.getMissingOrAmbiguousDetails()),
            assessment.getStructuredSymptomSummary(),
            assessment.getSuggestedDiagnosis(),
            assessment.getMedicalAttentionNote(),
            assessment.getStaffFacingExplanation(),
            assessment.getConfidenceLevel(),
            assessment.getAssessedAt()
        );
    }
}
