package com.careflowai.vector;

import com.careflowai.assessment.UrgencyAssessment;
import com.careflowai.assessment.UrgencyAssessmentRepository;
import com.careflowai.intake.Intake;
import com.careflowai.intake.IntakeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IntakeVectorBackfillRunner implements ApplicationRunner {

    private final IntakeRepository intakeRepository;
    private final UrgencyAssessmentRepository assessmentRepository;
    private final SimpleIntakeVectorStore vectorStore;

    public IntakeVectorBackfillRunner(IntakeRepository intakeRepository,
                                      UrgencyAssessmentRepository assessmentRepository,
                                      SimpleIntakeVectorStore vectorStore) {
        this.intakeRepository = intakeRepository;
        this.assessmentRepository = assessmentRepository;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        intakeRepository.findAll().forEach(this::indexIntake);
    }

    private void indexIntake(Intake intake) {
        try {
            UrgencyAssessment assessment = assessmentRepository
                .findTopByPatientIdOrderByAssessedAtDesc(intake.getPatient().getId())
                .orElse(null);
            vectorStore.indexIntake(intake, assessment);
        } catch (Exception ignored) {
            // Backfill should never prevent the clinical workspace from starting.
        }
    }
}
