package com.careflowai.thread.dto;

public record ThreadAttachmentRequest(
    String fileName,
    String fileType,
    String url
) {
}
