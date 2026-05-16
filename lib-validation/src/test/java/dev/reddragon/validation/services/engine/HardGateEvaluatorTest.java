package dev.reddragon.validation.services.engine;

import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.domain.models.CandidateValidationInput;
import dev.reddragon.domain.models.ReasonCode;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
