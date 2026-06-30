package com.careflowai.intake.dto;

import java.math.BigDecimal;

public record VitalsDto(
    BigDecimal temperatureC,
    Integer heartRate,
    Integer systolicPressure,
    Integer diastolicPressure,
    Integer respiratoryRate,
    Integer oxygenSaturation
) {
}
