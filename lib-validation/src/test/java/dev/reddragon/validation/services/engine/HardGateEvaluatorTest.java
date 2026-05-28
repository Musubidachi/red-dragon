package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.ReasonCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardGateEvaluatorTest {

    private final HardGateEvaluator evaluator = new HardGateEvaluator(
            ValidationThresholds.defaults()
    );

    @Test
    void returnsHardGateFailuresWhenCriticalConditionsFail() {
        CandidateValidationInput input = new CandidateValidationInput(
                "candidate",
                "FAIL",
                0.10,
                0.10,
                0.10,
                0.10,
                0.50,
                0.10,
                0.10,
                0.20,
                false,
                false,
                true,
                true,
                true,
                "hostile"
        );

        List<ReasonCode> failures = evaluator.process(input);

        assertTrue(failures.contains(ReasonCode.REQUIRED_DATA_MISSING));
        assertTrue(failures.contains(ReasonCode.CATALYST_NOT_CREDIBLE));
        assertTrue(failures.contains(ReasonCode.HARD_GATE_FAILED));
    }

    @Test
    void omitsHardGateMarkerWhenNoGateFails() {
        CandidateValidationInput input = new CandidateValidationInput(
                "candidate",
                "PASS",
                0.80,
                0.70,
                0.70,
                0.70,
                0.50,
                0.70,
                0.70,
                0.70,
                true,
                true,
                false,
                false,
                false,
                "clean"
        );

        List<ReasonCode> failures = evaluator.process(input);

        assertFalse(failures.contains(ReasonCode.HARD_GATE_FAILED));
    }
}
