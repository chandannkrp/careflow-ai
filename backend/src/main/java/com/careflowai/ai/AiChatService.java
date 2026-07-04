package com.careflowai.ai;

import com.careflowai.ai.dto.AiChatRequest;
import com.careflowai.ai.dto.AiChatResponse;
import com.careflowai.allocation.HospitalAllocationService;
import com.careflowai.allocation.dto.HospitalAllocationResponse;
import com.careflowai.metrics.QueueMetricsResponse;
import com.careflowai.metrics.QueueMetricsService;
import com.careflowai.queue.QueueService;
import com.careflowai.queue.dto.QueueEntryResponse;
import com.careflowai.staff.StaffUserService;
import com.careflowai.vector.SimpleIntakeVectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private static final String GENERIC_INSTRUCTION = """
        You are CareFlow AI's helpful assistant inside a hospital operations workspace.
        Answer general questions briefly and naturally in plain text.
        If the user asks about patients, treatment, diagnosis, queue, doctors, beds, or hospital records,
        use only supplied workspace context. If no context is supplied, say you can look that up from the workspace.
        """;

    private static final String RAG_INSTRUCTION = """
        You are CareFlow AI's staff operations assistant.
        Answer in plain natural English using only the supplied hospital context.
        You can discuss patient lookup, intake completeness, existing LLM triage summaries,
        urgency rationale already present in records, queue state, wait times, doctor assignments,
        beds, department coverage, and workflow actions.
        Do not invent patients, bookings, doctors, diagnoses, or treatment plans.
        If asked for new clinical diagnosis or care, state that a licensed clinician must decide
        and provide only the existing recorded triage facts.
        Keep answers concise and concrete.

        For patient detail, patient lookup, symptom, diagnosis, or treatment questions:
        - Use the closest relevant retrieved intake records only.
        - Prefer one patient unless the user asks for a list or comparison.
        - Do not mention vector scores, internal retrieval, raw payloads, database IDs, or fields marked unknown.
        - Omit unrelated closed/discharged/left-without-being-seen records unless the user asks for history.
        - Format exactly as short lines with these labels when the data exists:
          Patient:
          Triage:
          Attention:
          Queue:
          Assignment:
        - Keep each label to one sentence. Do not include extra sections.
        """;

    private final OpenAiResponsesClient responsesClient;
    private final SpringAiChatService springAiChatService;
    private final QueueMetricsService queueMetricsService;
    private final QueueService queueService;
    private final HospitalAllocationService hospitalAllocationService;
    private final StaffUserService staffUserService;
    private final SimpleIntakeVectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public AiChatService(OpenAiResponsesClient responsesClient,
                         SpringAiChatService springAiChatService,
                         QueueMetricsService queueMetricsService,
                         QueueService queueService,
                         HospitalAllocationService hospitalAllocationService,
                         StaffUserService staffUserService,
                         SimpleIntakeVectorStore vectorStore,
                         ObjectMapper objectMapper) {
        this.responsesClient = responsesClient;
        this.springAiChatService = springAiChatService;
        this.queueMetricsService = queueMetricsService;
        this.queueService = queueService;
        this.hospitalAllocationService = hospitalAllocationService;
        this.staffUserService = staffUserService;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    public AiChatResponse chat(AiChatRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (!requiresWorkspaceContext(message)) {
            return genericChat(request, message);
        }

        QueueMetricsResponse metrics = queueMetricsService.currentMetrics();
        List<SimpleIntakeVectorStore.SearchResult> intakeMatches = requiresSemanticRetrieval(message)
            ? vectorStore.search(message, 4)
            : List.of();
        List<QueueEntryResponse> queueContext = needsQueueContext(message)
            ? queueService.getQueue(null, null, null).stream().limit(8).toList()
            : List.of();
        HospitalAllocationResponse allocation = needsAllocationContext(message)
            ? hospitalAllocationService.currentAllocation()
            : null;

        try {
            String response = springAiChatService.respond(RAG_INSTRUCTION, objectMapper.writeValueAsString(
                chatPayload(request, metrics, intakeMatches, queueContext, allocation)
            ));
            if (response == null || response.isBlank()) {
                return fallbackResponse(request, metrics, intakeMatches, queueContext);
            }
            return new AiChatResponse(response, suggestedActions(message), true, Instant.now());
        } catch (Exception ignored) {
            if (!responsesClient.isAvailable()) {
                return fallbackResponse(request, metrics, intakeMatches, queueContext);
            }
            try {
                String response = responsesClient.respond(RAG_INSTRUCTION, objectMapper.writeValueAsString(
                    chatPayload(request, metrics, intakeMatches, queueContext, allocation)
                ));
                if (response == null || response.isBlank()) {
                    return fallbackResponse(request, metrics, intakeMatches, queueContext);
                }
                return new AiChatResponse(response, suggestedActions(message), true, Instant.now());
            } catch (Exception secondFailure) {
                return fallbackResponse(request, metrics, intakeMatches, queueContext);
            }
        }
    }

    private AiChatResponse genericChat(AiChatRequest request, String message) {
        try {
            String response = springAiChatService.respond(GENERIC_INSTRUCTION, objectMapper.writeValueAsString(Map.of(
                "staff", Map.of(
                    "name", request.actorName() == null ? "Unknown" : request.actorName(),
                    "role", request.actorRole() == null ? "Unknown" : request.actorRole()
                ),
                "message", message
            )));
            if (response != null && !response.isBlank()) {
                return new AiChatResponse(response, suggestedActions(message), true, Instant.now());
            }
        } catch (Exception ignored) {
            if (responsesClient.isAvailable()) {
                try {
                    String response = responsesClient.respond(GENERIC_INSTRUCTION, message);
                    if (response != null && !response.isBlank()) {
                        return new AiChatResponse(response, suggestedActions(message), true, Instant.now());
                    }
                } catch (Exception ignoredAgain) {
                    // Fall back below.
                }
            }
        }
        return new AiChatResponse(
            "I can help with general questions, or look up patient, queue, doctor, bed, and intake context when you ask about the workspace.",
            suggestedActions(message),
            false,
            Instant.now()
        );
    }

    private Map<String, Object> chatPayload(AiChatRequest request,
                                            QueueMetricsResponse metrics,
                                            List<SimpleIntakeVectorStore.SearchResult> intakeMatches,
                                            List<QueueEntryResponse> queueContext,
                                            HospitalAllocationResponse allocation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("staff", Map.of(
            "name", request.actorName() == null ? "Unknown" : request.actorName(),
            "role", request.actorRole() == null ? "Unknown" : request.actorRole()
        ));
        payload.put("message", request.message());
        payload.put("queueMetrics", metrics);
        if (!queueContext.isEmpty()) {
            payload.put("currentQueue", queueContext);
        }
        if (allocation != null) {
            payload.put("hospitalAllocation", allocation);
        }
        if (requiresStaffDirectory(request.message())) {
            payload.put("staffDirectory", staffUserService.list(null, null).stream().limit(12).toList());
        }
        if (!intakeMatches.isEmpty()) {
            payload.put("retrievedPatientIntakes", intakeMatches.stream().map(this::matchPayload).toList());
        }
        return payload;
    }

    private AiChatResponse fallbackResponse(AiChatRequest request, QueueMetricsResponse metrics,
                                            List<SimpleIntakeVectorStore.SearchResult> intakeMatches,
                                            List<QueueEntryResponse> queueContext) {
        String normalized = request.message() == null ? "" : request.message().toLowerCase();
        String message;
        if (normalized.contains("critical") || normalized.contains("urgent")) {
            message = "There are " + metrics.criticalAndHighWaiting()
                + " critical or high-priority patients waiting. Review the queue filter for Critical and High first.";
        } else if (normalized.contains("doctor") || normalized.contains("assign")) {
            message = doctorSummary(queueContext);
        } else if (normalized.contains("patient") || normalized.contains("booking") || normalized.contains("treatment")) {
            message = patientSummary(intakeMatches);
        } else if (normalized.contains("wait")) {
            message = "Longest waits by urgency are visible in the dashboard. Use the queue sort by wait time for row-level review.";
        } else if (normalized.contains("intake")) {
            message = "For intake, capture arrival mode, chief complaint, symptoms, vitals, distress level, risk flags, and department. Staff attribution is filled from the logged-in staff profile.";
        } else if (normalized.contains("status")) {
            message = "Use the status dropdown or Start action in the queue to move a patient into treatment. Metrics refresh after status changes.";
        } else {
            message = "I can help with queue status, intake completeness, wait-time visibility, prioritization rationale, and next workflow steps.";
        }
        return new AiChatResponse(message, suggestedActions(request.message()), false, Instant.now());
    }

    private Map<String, Object> matchPayload(SimpleIntakeVectorStore.SearchResult result) {
        return Map.of(
            "score", result.score(),
            "patientDisplayId", result.document().getPatientDisplayId(),
            "content", shortContent(result.document().getContent(), 900)
        );
    }

    private String patientSummary(List<SimpleIntakeVectorStore.SearchResult> intakeMatches) {
        if (intakeMatches.isEmpty()) {
            return "I could not find a matching patient intake record. Try a patient ID, complaint, symptom, diagnosis, or treatment keyword.";
        }
        SimpleIntakeVectorStore.SearchResult bestMatch = intakeMatches.get(0);
        return """
            Patient: %s
            Triage: %s
            Attention: Review the intake record and current queue placement before acting.
            Queue: Open the patient from the queue for current status and assignment.
            """.formatted(
            bestMatch.document().getPatientDisplayId(),
            shortContent(bestMatch.document().getContent(), 220)
        );
    }

    private String doctorSummary(List<QueueEntryResponse> queueContext) {
        List<String> assigned = queueContext.stream()
            .filter(entry -> entry.assignedDoctor() != null)
            .limit(5)
            .map(entry -> entry.patientDisplayId() + " is assigned to " + entry.assignedDoctor().displayName())
            .toList();
        if (assigned.isEmpty()) {
            return "No active queue patients have a doctor assignment visible right now.";
        }
        return "Current doctor assignments: " + String.join("; ", assigned) + ".";
    }

    private String shortContent(String content, int maxLength) {
        String compact = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxLength ? compact : compact.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private boolean requiresWorkspaceContext(String message) {
        return containsAny(message, List.of(
            "patient", "patients", "intake", "triage", "treatment", "doctor", "nurse", "staff", "bed",
            "queue", "urgency", "urgent", "critical", "high", "wait", "waiting", "chest", "pain", "symptom",
            "diagnosis", "attention", "blood", "bleeding", "breath", "pediatric", "trauma", "booking",
            "department", "allocation", "hospital"
        ));
    }

    private boolean requiresSemanticRetrieval(String message) {
        return containsAny(message, List.of(
            "patient", "patients", "intake", "triage", "treatment", "symptom", "diagnosis", "attention",
            "chest", "pain", "blood", "bleeding", "breath", "pediatric", "trauma", "complaint", "notes"
        ));
    }

    private boolean needsQueueContext(String message) {
        return containsAny(message, List.of(
            "queue", "urgency", "urgent", "critical", "high", "wait", "waiting", "doctor", "treatment",
            "patient", "patients", "triage", "status"
        ));
    }

    private boolean needsAllocationContext(String message) {
        return containsAny(message, List.of("bed", "beds", "doctor", "doctors", "allocation", "department", "treatment"));
    }

    private boolean requiresStaffDirectory(String message) {
        return containsAny(message, List.of("doctor", "doctors", "nurse", "staff", "directory", "assignment", "assigned"));
    }

    private boolean containsAny(String message, List<String> terms) {
        String normalized = message == null ? "" : message.toLowerCase();
        return terms.stream().anyMatch(normalized::contains);
    }

    private List<String> suggestedActions(String message) {
        String normalized = message == null ? "" : message.toLowerCase();
        if (normalized.contains("refresh") || normalized.contains("reload")) {
            return List.of("refresh_queue", "refresh_dashboard");
        }
        if (normalized.contains("critical") || normalized.contains("urgent")) {
            return List.of("filter_critical_high");
        }
        if (normalized.contains("intake")) {
            return List.of("open_intake");
        }
        return List.of();
    }
}
