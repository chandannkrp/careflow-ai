package com.careflowai.queue.dto;

import com.careflowai.common.StaffRole;

public record RemoveQueueEntryRequest(
    String actorName,
    StaffRole actorRole,
    String reason
) {
}
