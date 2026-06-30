package com.careflowai.common;

public enum UrgencyCategory {
    CRITICAL(0, 90, 100),
    HIGH(1, 70, 89),
    MEDIUM(2, 40, 69),
    LOW(3, 0, 39);

    private final int sortOrder;
    private final int minScore;
    private final int maxScore;

    UrgencyCategory(int sortOrder, int minScore, int maxScore) {
        this.sortOrder = sortOrder;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static UrgencyCategory fromScore(int score) {
        if (score >= CRITICAL.minScore) {
            return CRITICAL;
        }
        if (score >= HIGH.minScore) {
            return HIGH;
        }
        if (score >= MEDIUM.minScore) {
            return MEDIUM;
        }
        return LOW;
    }
}
