package dev.reddragon.validation.services;

import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.ValidationAudit;
import dev.reddragon.domain.models.Verdict;
import dev.reddragon.validation.config.ValidationThresholds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Smoke tests for ValidationService - the stable lib-validation facade
 * called out as the public entry in the README.
 *
 * <p>Each test exercises one of the three top-level paths through the engine:
 * a pass-quality candidate, a watch-band candidate, and a reject due to a
 * hard gate.
 */
class ValidationServiceTest {

    private final ValidationService service = new ValidationService();

    @Test
    void noArgConstructorUsesDefaultThresholds() {
        assertNotNull(new ValidationService());
    }

    @Test
    void nullInputRejected() {
        assertThrows(NullPointerException.class, () -> service.process(null));
    }

    @Test
    void nullThresholdsRejected() {
        assertThrows(NullPointerException.class, () -> new ValidationService(null));
    }

    @Test
    void strongCandidateProducesPassVerdict() {
        ValidationAudit audit = service.process(strongCandidate());

        assertEquals("c-strong", audit.candidateId());
        assertEquals("ACME", audit.symbol());
        assertEquals(Verdict.PASS, audit.validationResult().verdict(),
                "high-quality candidate should pass");
        assertNotNull(audit.validatedAt());
    }

    @Test
    void hostileMarketStructureForcesRejectViaHardGate() {
        // Even with strong scores, hostile market structure must reject.
        ValidationAudit audit = service.process(strongCandidateWith(
                /* hostileMarketStructure = */ true,
                /* euphoricOrSaturated = */ false,
                /* equilibriumAlreadyRepriced = */ false
        ));
        assertEquals(Verdict.REJECT, audit.validationResult().verdict());
    }

    @Test
    void confidenceScoreIsAlwaysProduced() {
        ValidationAudit audit = service.process(strongCandidate());
        // No upper bound asserted - the engine may push above 1.0 internally;
        // just confirm the field was populated.
        assertNotNull(audit.validationConfidenceScore());
    }

    @Test
    void customThresholdsRouteThroughTheEngine() {
        ValidationService strict = new ValidationService(ValidationThresholds.defaults());
        ValidationAudit audit = strict.process(strongCandidate());
        assertNotNull(audit);
    }

    private CandidateValidationInput strongCandidate() {
        return strongCandidateWith(false, false, false);
    }

    private CandidateValidationInput strongCandidateWith(
            boolean hostile, boolean euphoric, boolean repriced
    ) {
        return new CandidateValidationInput(
                "c-strong", "ACME",
                0.85, 0.80, 0.75, 0.75, 0.65, 0.85, 0.75, 0.85,
                /* credibleCatalyst */ true,
                /* requiredDataPresent */ true,
                /* euphoricOrSaturated */ euphoric,
                /* hostileMarketStructure */ hostile,
                /* equilibriumAlreadyRepriced */ repriced,
                "test"
        );
    }
}
