package com.careflowai.intake;

import jakarta.persistence.Embeddable;

@Embeddable
public class RiskFlags {

    private boolean chestPain;
    private boolean breathingDifficulty;
    private boolean alteredMentalState;
    private boolean severeBleeding;
    private boolean pregnancy;
    private boolean pediatricRisk;
    private boolean fallOrTrauma;
    private boolean immunocompromised;

    public boolean isChestPain() {
        return chestPain;
    }

    public void setChestPain(boolean chestPain) {
        this.chestPain = chestPain;
    }

    public boolean isBreathingDifficulty() {
        return breathingDifficulty;
    }

    public void setBreathingDifficulty(boolean breathingDifficulty) {
        this.breathingDifficulty = breathingDifficulty;
    }

    public boolean isAlteredMentalState() {
        return alteredMentalState;
    }

    public void setAlteredMentalState(boolean alteredMentalState) {
        this.alteredMentalState = alteredMentalState;
    }

    public boolean isSevereBleeding() {
        return severeBleeding;
    }

    public void setSevereBleeding(boolean severeBleeding) {
        this.severeBleeding = severeBleeding;
    }

    public boolean isPregnancy() {
        return pregnancy;
    }

    public void setPregnancy(boolean pregnancy) {
        this.pregnancy = pregnancy;
    }

    public boolean isPediatricRisk() {
        return pediatricRisk;
    }

    public void setPediatricRisk(boolean pediatricRisk) {
        this.pediatricRisk = pediatricRisk;
    }

    public boolean isFallOrTrauma() {
        return fallOrTrauma;
    }

    public void setFallOrTrauma(boolean fallOrTrauma) {
        this.fallOrTrauma = fallOrTrauma;
    }

    public boolean isImmunocompromised() {
        return immunocompromised;
    }

    public void setImmunocompromised(boolean immunocompromised) {
        this.immunocompromised = immunocompromised;
    }
}
