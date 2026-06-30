package com.careflowai.assessment;

import com.careflowai.common.UrgencyCategory;
import java.util.List;

public record ScoreResult(
    int score,
    UrgencyCategory category,
    List<String> factors
) {
}
