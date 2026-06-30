package com.careflowai.thread;

import com.careflowai.thread.dto.CreateThreadCommentRequest;
import com.careflowai.thread.dto.ThreadCommentResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PatientThreadController {

    private final PatientThreadService patientThreadService;

    public PatientThreadController(PatientThreadService patientThreadService) {
        this.patientThreadService = patientThreadService;
    }

    @GetMapping("/patients/{patientId}/thread")
    public List<ThreadCommentResponse> list(@PathVariable UUID patientId) {
        return patientThreadService.list(patientId);
    }

    @PostMapping("/intakes/{intakeId}/thread")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadCommentResponse create(@PathVariable UUID intakeId,
                                        @Valid @RequestBody CreateThreadCommentRequest request) {
        return patientThreadService.create(intakeId, request);
    }
}
