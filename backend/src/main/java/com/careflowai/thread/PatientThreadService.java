package com.careflowai.thread;

import com.careflowai.agent.PatientTimelineEvent;
import com.careflowai.agent.PatientTimelineEventRepository;
import com.careflowai.intake.Intake;
import com.careflowai.intake.IntakeRepository;
import com.careflowai.thread.dto.CreateThreadCommentRequest;
import com.careflowai.thread.dto.ThreadAttachmentRequest;
import com.careflowai.thread.dto.ThreadCommentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientThreadService {

    private final IntakeRepository intakeRepository;
    private final PatientThreadCommentRepository commentRepository;
    private final PatientTimelineEventRepository timelineRepository;

    public PatientThreadService(IntakeRepository intakeRepository,
                                PatientThreadCommentRepository commentRepository,
                                PatientTimelineEventRepository timelineRepository) {
        this.intakeRepository = intakeRepository;
        this.commentRepository = commentRepository;
        this.timelineRepository = timelineRepository;
    }

    @Transactional(readOnly = true)
    public List<ThreadCommentResponse> list(UUID patientId) {
        return commentRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream()
            .map(ThreadCommentResponse::from)
            .toList();
    }

    @Transactional
    public ThreadCommentResponse create(UUID intakeId, CreateThreadCommentRequest request) {
        Intake intake = intakeRepository.findById(intakeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake not found."));
        PatientThreadComment comment = new PatientThreadComment(
            intake.getPatient(),
            intake,
            request.authorName().trim(),
            request.body().trim()
        );
        if (request.attachments() != null) {
            request.attachments().stream()
                .filter(this::hasAttachment)
                .forEach(attachment -> comment.addAttachment(new PatientThreadAttachment(
                    attachment.fileName().trim(),
                    cleanOptional(attachment.fileType()),
                    attachment.url().trim()
                )));
        }
        PatientThreadComment saved = commentRepository.save(comment);
        timelineRepository.save(new PatientTimelineEvent(
            intake.getPatient(),
            intake,
            null,
            "THREAD_COMMENT",
            "Thread comment added",
            "%s commented: %s".formatted(request.authorName().trim(), request.body().trim()),
            "STAFF"
        ));
        return ThreadCommentResponse.from(saved);
    }

    private boolean hasAttachment(ThreadAttachmentRequest attachment) {
        return attachment != null
            && StringUtils.hasText(attachment.fileName())
            && StringUtils.hasText(attachment.url());
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
