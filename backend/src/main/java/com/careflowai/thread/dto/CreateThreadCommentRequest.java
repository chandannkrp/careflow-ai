package com.careflowai.thread.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateThreadCommentRequest(
    @NotBlank String authorName,
    @NotBlank String body,
    List<ThreadAttachmentRequest> attachments
) {
}
