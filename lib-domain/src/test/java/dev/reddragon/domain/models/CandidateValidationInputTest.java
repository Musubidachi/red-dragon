package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CandidateValidationInputTest {

    @Test
    void builderRejectsOutOfRangeInputScores() {
        CandidateValidationInput.CandidateValidationInputBuilder builder = validBuilder();

        assertThrows(IllegalArgumentException.class,
                () -> builder.structuralRealityScore(1.01).build());
    }

    @Test
    void builderKeepsNormalizedScoresUnchanged() {
        CandidateValidationInput input = validBuilder()
                .deploymentConfidenceScore(0.42)
                .build();

        assertEquals(0.42, input.deploymentConfidenceScore());
    }

    private CandidateValidationInput.CandidateValidationInputBuilder validBuilder() {
        return CandidateValidationInput.builder()
                .candidateId("candidate-1")
                .symbol("ACME")
                .structuralRealityScore(0.7)
                .materialSignificanceScore(0.7)
                .earlynessScore(0.7)
                .equilibriumQualityScore(0.7)
                .reflexivityPotentialScore(0.7)
                .asymmetryScore(0.7)
                .regimeCompatibilityScore(0.7)
                .deploymentConfidenceScore(0.7)
                .credibleCatalyst(true)
                .requiredDataPresent(true);
    }
}
