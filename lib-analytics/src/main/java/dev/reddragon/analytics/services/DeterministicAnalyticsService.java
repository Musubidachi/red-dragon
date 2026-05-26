package dev.reddragon.analytics.services;

import dev.reddragon.analytics.services.classification.EquilibriumQualityScorer;
import dev.reddragon.analytics.services.classification.RegimeCompatibilityScorer;
import dev.reddragon.analytics.services.deployment.DeploymentConfidenceScorer;
import dev.reddragon.analytics.services.propagation.PropagationPhaseAnalyzer;
import dev.reddragon.analytics.services.propagation.ReflexivityScorer;
import dev.reddragon.analytics.services.structural.AdversarialValidationAnalyzer;
import dev.reddragon.analytics.services.structural.AsymmetryScorer;
import dev.reddragon.domain.models.AdversarialFinding;
import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.PhaseLabel;
import dev.reddragon.domain.models.PhaseTransitionSnapshot;
import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.domain.models.TradeCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Produces deterministic analytics features from a candidate and market data.
 *
 * <p>This orchestrator is intentionally thin — it composes the documented
 * L3/L4/L5/L6 scorers in the order described by {@code ARCHITECTURE.md} and
 * does not embed scoring logic of its own. The previous inline math has been
 * deleted in favour of delegation so the standalone scorer endpoints
 * (e.g. {@code POST /api/market-state/classify}) and the candidate pipeline
 * cannot drift apart.
 *
 * <p>The orchestrator only has a {@link TradeCandidate} and a
 * {@link MarketDataSnapshot} to work with; the richer adversarial analysis
 * also requires intraday-structure, liquidity-texture, propagation,
 * fundamental-impact, volatility-expansion, and options-flow snapshots. For
 * the pipeline path we pass {@code null} for those snapshots;
 * {@link AdversarialValidationAnalyzer#process} silently skips checks whose
 * required snapshots are missing. Full-data callers that construct all
 * eight snapshots get the full ten-check pass.
 *
 * <p>A lightweight adversarial pre-pass also runs every time, using the
 * orchestrator's derived {@code reflexivity} and the
 * {@link MarketDataSnapshot}'s liquidity/volatility/rangePosition fields.
 * These three checks are not redundant with the analyzer because they
 * consume different inputs (the analyzer reads richer snapshot types).
 */
public class DeterministicAnalyticsService {

    private final RegimeCompatibilityScorer regimeCompatibilityScorer;
    private final EquilibriumQualityScorer equilibriumQualityScorer;
    private final AsymmetryScorer asymmetryScorer;
    private final ReflexivityScorer reflexivityScorer;
    private final DeploymentConfidenceScorer deploymentConfidenceScorer;
    private final AdversarialValidationAnalyzer adversarialValidationAnalyzer;
    private final PropagationPhaseAnalyzer propagationPhaseAnalyzer;

    /**
     * Convenience constructor that news-up default instances of every scorer.
     * Used by Spring wiring and by test fixtures that don't want to construct
     * the full dependency graph by hand.
     */
    public DeterministicAnalyticsService() {
        this(
                new RegimeCompatibilityScorer(),
                new EquilibriumQualityScorer(),
                new AsymmetryScorer(),
                new ReflexivityScorer(),
                new DeploymentConfidenceScorer(),
                new AdversarialValidationAnalyzer(),
                new PropagationPhaseAnalyzer()
        );
    }

    public DeterministicAnalyticsService(
            RegimeCompatibilityScorer regimeCompatibilityScorer,
            EquilibriumQualityScorer equilibriumQualityScorer,
            AsymmetryScorer asymmetryScorer,
            ReflexivityScorer reflexivityScorer,
            DeploymentConfidenceScorer deploymentConfidenceScorer,
            AdversarialValidationAnalyzer adversarialValidationAnalyzer,
            PropagationPhaseAnalyzer propagationPhaseAnalyzer
    ) {
        this.regimeCompatibilityScorer = Objects.requireNonNull(regimeCompatibilityScorer, "regimeCompatibilityScorer is required");
        this.equilibriumQualityScorer = Objects.requireNonNull(equilibriumQualityScorer, "equilibriumQualityScorer is required");
        this.asymmetryScorer = Objects.requireNonNull(asymmetryScorer, "asymmetryScorer is required");
        this.reflexivityScorer = Objects.requireNonNull(reflexivityScorer, "reflexivityScorer is required");
        this.deploymentConfidenceScorer = Objects.requireNonNull(deploymentConfidenceScorer, "deploymentConfidenceScorer is required");
        this.adversarialValidationAnalyzer = Objects.requireNonNull(adversarialValidationAnalyzer, "adversarialValidationAnalyzer is required");
        this.propagationPhaseAnalyzer = Objects.requireNonNull(propagationPhaseAnalyzer, "propagationPhaseAnalyzer is required");
    }

    /**
     * Main processing flow. Composes the L4 regime classification, L3 asymmetry
     * + adversarial checks, L4 equilibrium quality, L6 reflexivity, L5 deployment
     * confidence, and L6 propagation phase into a single {@link AnalyticsSnapshot}.
     */
    public AnalyticsSnapshot process(
            TradeCandidate candidate,
            MarketDataSnapshot marketData
    ) {
        validate(candidate, marketData);

        List<String> notes = new ArrayList<>();

        // L4 — classify the regime and translate to a compatibility score
        RegimeLabel regime = regimeCompatibilityScorer.process(marketData, notes);
        double regimeCompatibility = regimeCompatibilityScorer.score(regime);

        // L4 — usable-for-restoration score
        double equilibriumQuality = equilibriumQualityScorer.process(marketData, notes);

        // L3 — asymmetry consumes equilibrium quality as a fourth dimension
        double asymmetry = asymmetryScorer.process(candidate, marketData, equilibriumQuality, notes);

        // L6 — reflexivity (propagation potential)
        double reflexivity = reflexivityScorer.process(candidate, notes);

        // L5 — confidence input (lib-validation owns the final tier decision)
        double deploymentConfidence = deploymentConfidenceScorer.process(
                candidate,
                asymmetry,
                equilibriumQuality,
                regimeCompatibility,
                reflexivity,
                notes
        );

        // L6 — propagation phase from earlyness → reflexivity slope
        PhaseLabel phase = propagationPhase(candidate, reflexivity);
        notes.add("Propagation phase: " + phase.name());

        // L3 — adversarial checks. Two passes:
        //   1. AdversarialValidationAnalyzer runs the rich-data checks; it
        //      gracefully skips checks whose snapshots the pipeline does
        //      not have.
        //   2. A lightweight pass below covers the candidate-only / pipeline-
        //      only adversarial flags that the analyzer's check set does not
        //      duplicate (it uses derived reflexivity and MarketDataSnapshot
        //      fields rather than the richer snapshot types).
        appendAdversarialFindings(candidate, marketData, notes);
        appendLightweightAdversarialNotes(candidate, marketData, reflexivity, notes);

        return buildSnapshot(
                candidate,
                marketData,
                notes,
                regime,
                regimeCompatibility,
                asymmetry,
                equilibriumQuality,
                reflexivity,
                deploymentConfidence
        );
    }

    /**
     * Derives a {@link PhaseLabel} from the candidate's earlyness and reflexivity
     * scores via {@link PropagationPhaseAnalyzer}.
     */
    private PhaseLabel propagationPhase(TradeCandidate candidate, double currentReflexivity) {
        double previous = candidate.earlynessScore();
        double current  = currentReflexivity;
        double slope        = current - previous;
        // Half-scale the slope as a coarse proxy for the second derivative.
        double acceleration = slope * 0.5;
        PhaseTransitionSnapshot snapshot = new PhaseTransitionSnapshot(previous, current, slope, acceleration);
        return propagationPhaseAnalyzer.process(snapshot);
    }

    /**
     * Run {@link AdversarialValidationAnalyzer} with the data the orchestrator
     * has. {@link AdversarialValidationAnalyzer#process} silently skips checks
     * whose required snapshots are missing, so passing {@code null} for the
     * snapshots the pipeline does not have is the right contract — the few
     * checks that need only {@code TradeCandidate} + {@code MarketDataSnapshot}
     * still fire (today the analyzer needs at least one of the optional
     * snapshots to run any check, so the pipeline path is silent until those
     * are wired in; the standalone {@code /api/analysis/{ticker}} flow does
     * construct the richer snapshots and gets the full ten-check pass).
     *
     * <p>The {@link AdversarialFinding}s that do fire are appended to {@code
     * notes} so the trader review surface sees them alongside the other
     * orchestrator-produced narrative.
     */
    private void appendAdversarialFindings(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            List<String> notes
    ) {
        List<AdversarialFinding> findings = adversarialValidationAnalyzer.process(
                candidate,
                marketData,
                null,   // IntradayStructureSnapshot — not available in pipeline path
                null,   // LiquidityTextureSnapshot
                null,   // PropagationSnapshot
                null,   // FundamentalImpactSnapshot
                null,   // VolatilityExpansionSnapshot
                null    // OptionsFlowSnapshot
        );
        for (AdversarialFinding finding : findings) {
            notes.add("Adversarial flag (" + finding.type().name() + "): " + finding.explanation());
        }
    }

    // ---- Lightweight adversarial-note triggers -----------------------------
    // These run on the candidate + derived reflexivity + MarketDataSnapshot
    // data the orchestrator has. They are NOT redundant with
    // AdversarialValidationAnalyzer because that analyzer uses richer
    // snapshot types (PropagationSnapshot, LiquidityTextureSnapshot, etc.)
    // and only fires when those are supplied. These three checks fire
    // whenever any candidate flows through the pipeline.
    private static final double LITE_HYPE_STRUCTURAL_MAX           = 0.45;
    private static final double LITE_HYPE_REFLEXIVITY_MIN          = 0.70;
    private static final double LITE_LATE_ENTRY_EARLYNESS_MAX      = 0.40;
    private static final double LITE_LATE_ENTRY_RANGE_MIN          = 0.85;
    private static final double LITE_LIQUIDITY_DETERIORATION_FLOOR = 0.35;

    private void appendLightweightAdversarialNotes(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            double reflexivity,
            List<String> notes
    ) {
        if (candidate.structuralRealityScore() < LITE_HYPE_STRUCTURAL_MAX
                && reflexivity > LITE_HYPE_REFLEXIVITY_MIN) {
            notes.add("Adversarial flag: reflexivity is elevated but structural reality is weak; possible hype without substance.");
        }
        if (candidate.earlynessScore() < LITE_LATE_ENTRY_EARLYNESS_MAX
                && marketData.rangePosition() > LITE_LATE_ENTRY_RANGE_MIN) {
            notes.add("Adversarial flag: earlyness is low and price is near range high; late-entry risk elevated.");
        }
        if (marketData.liquidityScore() < LITE_LIQUIDITY_DETERIORATION_FLOOR
                && marketData.volatilityStabilityScore() < LITE_LIQUIDITY_DETERIORATION_FLOOR) {
            notes.add("Adversarial flag: both liquidity and volatility stability are degraded; adverse execution risk.");
        }
    }

    private void validate(
            TradeCandidate candidate,
            MarketDataSnapshot marketData
    ) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate is required");
        }
        if (marketData == null) {
            throw new IllegalArgumentException("marketData is required");
        }
    }

    private AnalyticsSnapshot buildSnapshot(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            List<String> notes,
            RegimeLabel regime,
            double regimeCompatibility,
            double asymmetry,
            double equilibriumQuality,
            double reflexivity,
            double deploymentConfidence
    ) {
        // Anchor analytics to the market-data observation timestamp so identical
        // inputs produce identical snapshots (backtests, replays, idempotency).
        return new AnalyticsSnapshot(
                candidate.candidateId(),
                candidate.symbol(),
                marketData.observedAt(),
                regime,
                regimeCompatibility,
                asymmetry,
                equilibriumQuality,
                reflexivity,
                deploymentConfidence,
                notes
        );
    }

}
