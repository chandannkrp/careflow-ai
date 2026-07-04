package com.careflowai.queue.dto;

import com.careflowai.agent.CareTeamAssignment;
import com.careflowai.common.QueueStatus;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.queue.QueueEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record QueueEntryResponse(
    UUID patientId,
    UUID intakeId,
    String patientDisplayId,
    UrgencyCategory urgencyCategory,
    int urgencyScore,
    String chiefComplaint,
    long waitingMinutes,
    String waitPriorityLevel,
    boolean waitThresholdExceeded,
    String department,
    QueueStatus status,
    boolean staffEscalated,
    Instant waitingSince,
    AssignedDoctorResponse assignedDoctor
) {
    public static QueueEntryResponse from(QueueEntry entry, Instant now) {
        return from(entry, now, null);
    }

    public static QueueEntryResponse from(QueueEntry entry, Instant now, CareTeamAssignment assignment) {
        long waitingMinutes = Math.max(0, Duration.between(entry.getWaitingSince(), now).toMinutes());
        return new QueueEntryResponse(
            entry.getPatient().getId(),
            entry.getIntake().getId(),
            entry.getPatient().getDisplayId(),
            entry.getUrgencyCategory(),
            entry.getUrgencyScore(),
            entry.getIntake().getChiefComplaint(),
            waitingMinutes,
            waitPriorityLevel(waitingMinutes),
            waitingMinutes >= 30,
            entry.getDepartment(),
            entry.getStatus(),
            entry.isStaffEscalated(),
            entry.getWaitingSince(),
            AssignedDoctorResponse.from(assignment)
        );
    }

    private static String waitPriorityLevel(long waitingMinutes) {
        if (waitingMinutes >= 40) {
            return "OVER_TARGET";
        }
        if (waitingMinutes >= 30) {
            return "NEAR_TARGET";
        }
        return "ON_TRACK";
    }
}
