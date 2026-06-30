package com.careflowai.thread.dto;

import com.careflowai.thread.PatientThreadAttachment;
import java.util.UUID;

public record ThreadAttachmentResponse(
    UUID id,
    String fileName,
    String fileType,
    String url
) {
    public static ThreadAttachmentResponse from(PatientThreadAttachment attachment) {
        return new ThreadAttachmentResponse(
            attachment.getId(),
            attachment.getFileName(),
            attachment.getFileType(),
            attachment.getUrl()
        );
    }
}
