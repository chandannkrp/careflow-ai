package com.careflowai.agent.dto;

import java.util.List;

public record AgentDashboardResponse(
    List<PatientFlashcardResponse> flashcards,
    List<PatientTimelineEventResponse> timeline
) {
}
