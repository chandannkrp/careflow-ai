package com.careflowai.intake.dto;

public record RiskFlagsDto(
    boolean chestPain,
    boolean breathingDifficulty,
    boolean alteredMentalState,
    boolean severeBleeding,
    boolean pregnancy,
    boolean pediatricRisk,
    boolean fallOrTrauma,
    boolean immunocompromised
) {
}
