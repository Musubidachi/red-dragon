package dev.reddragon.app.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.analytics.models.exit.ExitSignal;
import dev.reddragon.analytics.models.exit.ExitSignalInput;
import dev.reddragon.analytics.services.exit.EquilibriumCompressionScorer;
import lombok.RequiredArgsConstructor;

/**
 * MD Layer 7 exit signal endpoint. Given the current state of an open
 * position (entry asymmetry, current asymmetry, equilibrium and propagation
 * phases, range position), returns a HOLD / TIGHTEN / SCALE_OUT / EXIT_NOW
 * recommendation with reasoning.
 *
 * <pre>
 * POST /api/exit-signal
 * {
 *   "equilibriumPhase": "SATURATION_RISK",
 *   "propagationPhase": "LATE_REFLEXIVITY",
 *   "currentAsymmetry": 0.35,
 *   "entryAsymmetry":   0.82,
 *   "rangePosition":    0.92,
 *   "nearRecentHigh":   true
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/exit-signal")
@RequiredArgsConstructor
public class ExitSignalController {

    private final EquilibriumCompressionScorer scorer;

    @PostMapping
    public ExitSignal evaluate(@RequestBody ExitSignalInput input) {
        return scorer.process(input);
    }
}
