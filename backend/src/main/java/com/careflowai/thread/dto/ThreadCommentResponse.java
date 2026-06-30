package com.careflowai.thread.dto;

import com.careflowai.thread.PatientThreadComment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreadCommentResponse(
    UUID id,
    UUID patientId,
    UUID intakeId,
    String patientDisplayId,
    String authorName,
    String body,
    List<ThreadAttachmentResponse> attachments,
    Instant createdAt
) {
    public static ThreadCommentResponse from(PatientThreadComment comment) {
        return new ThreadCommentResponse(
            comment.getId(),
            comment.getPatient().getId(),
            comment.getIntake().getId(),
            comment.getPatient().getDisplayId(),
            comment.getAuthorName(),
            comment.getBody(),
            comment.getAttachments().stream().map(ThreadAttachmentResponse::from).toList(),
            comment.getCreatedAt()
        );
    }
}
