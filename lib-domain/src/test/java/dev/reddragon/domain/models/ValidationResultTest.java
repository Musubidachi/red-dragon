package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ValidationResult - the lib-validation output that feeds the
 * trader review surface.
 */
class ValidationResultTest {

    @Test
    void passedRejectedWatchHelpersMirrorTheVerdict() {
        assertTrue(result(Verdict.PASS).passed());
        assertTrue(result(Verdict.REJECT).rejected());
        assertTrue(result(Verdict.WATCH).watch());
    }

    @Test
    void isActionableFollowsDeploymentTier() {
        ValidationResult standard = result(Verdict.PASS, DeploymentTier.STANDARD);
        ValidationResult observe  = result(Verdict.WATCH, DeploymentTier.OBSERVE);
        assertTrue(standard.isActionable());
        assertFalse(observe.isActionable());
    }

    @Test
    void primaryReasonFallsBackWhenNoCodes() {
        ValidationResult r = new ValidationResult(
                "id", "ABC", Verdict.WATCH, DeploymentTier.PROBE, 0.5,
                List.of(), List.of(), List.of()
        );
        assertEquals("No reason codes", r.primaryReason());
    }

    @Test
    void primaryReasonUsesFirstReasonCodeDisplayName() {
        ValidationResult r = new ValidationResult(
                "id", "ABC", Verdict.PASS, DeploymentTier.STANDARD, 0.8,
                List.of(), List.of(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED, ReasonCode.MATERIAL_IMPACT_HIGH),
                List.of()
        );
        assertEquals(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED.displayName(), r.primaryReason());
    }

    @Test
    void outOfRangeScoreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ValidationResult(
                "id", "ABC", Verdict.PASS, DeploymentTier.STANDARD, 1.5,
                List.of(), List.of(), List.of()
        ));
    }

    private ValidationResult result(Verdict v) {
        return result(v, DeploymentTier.STANDARD);
    }

    private ValidationResult result(Verdict v, DeploymentTier tier) {
        return new ValidationResult(
                "id", "ABC", v, tier, 0.7,
                List.of(), List.of(), List.of()
        );
    }
}
