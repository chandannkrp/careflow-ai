package com.careflowai.assessment;

import com.careflowai.common.AgeBand;
import com.careflowai.common.ArrivalMode;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.intake.Intake;
import com.careflowai.intake.RiskFlags;
import com.careflowai.intake.Vitals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UrgencyScoringService {

    public ScoreResult score(Intake intake) {
        int score = 10;
        List<String> factors = new ArrayList<>();
        factors.add("Baseline intake priority: +10");

        score += scoreRiskFlags(intake.getRiskFlags(), factors);
        score += scoreVitals(intake.getVitals(), factors);
        score += scorePain(intake.getPainLevel(), factors);
        score += scoreAgeBand(intake.getPatient().getAgeBand(), factors);
        score += scoreArrivalMode(intake.getArrivalMode(), factors);

        int boundedScore = Math.max(0, Math.min(100, score));
        return new ScoreResult(boundedScore, UrgencyCategory.fromScore(boundedScore), factors);
    }

    private int scoreRiskFlags(RiskFlags flags, List<String> factors) {
        int score = 0;
        if (flags.isSevereBleeding()) {
            score += 50;
            factors.add("Severe bleeding risk flag: +50");
        }
        if (flags.isAlteredMentalState()) {
            score += 45;
            factors.add("Altered mental state risk flag: +45");
        }
        if (flags.isBreathingDifficulty()) {
            score += 35;
            factors.add("Breathing difficulty risk flag: +35");
        }
        if (flags.isChestPain()) {
            score += 30;
            factors.add("Chest pain risk flag: +30");
        }
        if (flags.isPediatricRisk()) {
            score += 20;
            factors.add("Pediatric risk flag: +20");
        }
        if (flags.isPregnancy()) {
            score += 15;
            factors.add("Pregnancy risk flag: +15");
        }
        if (flags.isFallOrTrauma()) {
            score += 15;
            factors.add("Fall or trauma risk flag: +15");
        }
        if (flags.isImmunocompromised()) {
            score += 15;
            factors.add("Immunocompromised risk flag: +15");
        }
        return score;
    }

    private int scoreVitals(Vitals vitals, List<String> factors) {
        int score = 0;
        Integer oxygen = vitals.getOxygenSaturation();
        if (oxygen != null && oxygen < 90) {
            score += 45;
            factors.add("Oxygen saturation below 90%: +45");
        } else if (oxygen != null && oxygen <= 93) {
            score += 25;
            factors.add("Oxygen saturation 90-93%: +25");
        }

        Integer heartRate = vitals.getHeartRate();
        if (heartRate != null && heartRate > 130) {
            score += 30;
            factors.add("Heart rate above 130 bpm: +30");
        } else if (heartRate != null && heartRate > 110) {
            score += 15;
            factors.add("Heart rate above 110 bpm: +15");
        }

        Integer respiratoryRate = vitals.getRespiratoryRate();
        if (respiratoryRate != null && respiratoryRate >= 30) {
            score += 30;
            factors.add("Respiratory rate 30 or higher: +30");
        } else if (respiratoryRate != null && respiratoryRate > 22) {
            score += 15;
            factors.add("Respiratory rate above 22: +15");
        }

        Integer systolic = vitals.getSystolicPressure();
        if (systolic != null && systolic < 90) {
            score += 35;
            factors.add("Systolic pressure below 90 mmHg: +35");
        }

        BigDecimal temperature = vitals.getTemperatureC();
        if (temperature != null && temperature.compareTo(new BigDecimal("39.0")) >= 0) {
            score += 15;
            factors.add("Temperature 39.0 C or higher: +15");
        }
        return score;
    }

    private int scorePain(int painLevel, List<String> factors) {
        if (painLevel >= 8) {
            factors.add("Pain level 8-10: +20");
            return 20;
        }
        if (painLevel >= 5) {
            factors.add("Pain level 5-7: +10");
            return 10;
        }
        return 0;
    }

    private int scoreAgeBand(AgeBand ageBand, List<String> factors) {
        if (ageBand == AgeBand.OLDER_ADULT) {
            factors.add("Older adult age band: +8");
            return 8;
        }
        if (ageBand == AgeBand.CHILD) {
            factors.add("Child age band: +6");
            return 6;
        }
        return 0;
    }

    private int scoreArrivalMode(ArrivalMode arrivalMode, List<String> factors) {
        if (arrivalMode == ArrivalMode.AMBULANCE) {
            factors.add("Ambulance arrival: +10");
            return 10;
        }
        if (arrivalMode == ArrivalMode.TRANSFER) {
            factors.add("Transfer arrival: +5");
            return 5;
        }
        return 0;
    }
}
