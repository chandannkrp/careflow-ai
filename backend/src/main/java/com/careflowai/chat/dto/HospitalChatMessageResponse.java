package com.careflowai.chat.dto;

import com.careflowai.common.StaffRole;
import java.time.Instant;

public record HospitalChatMessageResponse(
    String id,
    String authorName,
    StaffRole authorRole,
    String body,
    boolean savi,
    Instant createdAt
) {
}
