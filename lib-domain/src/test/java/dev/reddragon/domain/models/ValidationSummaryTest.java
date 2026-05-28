package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationSummaryTest {

    @Test
    void validationScoresRejectOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> summary(1.2, 0.7));
        assertThrows(IllegalArgumentException.class, () -> summary(0.7, -0.1));
    }

    @Test
    void nullTextListsNormalizeForDisplay() {
        ValidationSummary summary = new ValidationSummary(
                "id",
                "ABC",
                Verdict.WATCH,
                DeploymentTier.PROBE,
                0.7,
                0.6,
                null,
                null,
                null
        );

        assertEquals(List.of(), summary.riskFlags());
        assertEquals("", summary.headline());
        assertEquals("", summary.summaryText());
    }

    private ValidationSummary summary(double score, double validationConfidenceScore) {
        return new ValidationSummary(
                "id",
                "ABC",
                Verdict.PASS,
                DeploymentTier.STANDARD,
                score,
                validationConfidenceScore,
                List.of(),
                "headline",
                "summary"
        );
    }
}
