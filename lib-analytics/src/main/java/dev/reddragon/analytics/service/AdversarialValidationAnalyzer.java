package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.AdversarialFinding;
import dev.reddragon.analytics.model.AdversarialFindingType;
import dev.reddragon.analytics.model.FundamentalImpactSnapshot;
import dev.reddragon.analytics.model.IntradayStructureSnapshot;
import dev.reddragon.analytics.model.LiquidityTextureSnapshot;
import dev.reddragon.analytics.model.OptionsFlowSnapshot;
import dev.reddragon.analytics.model.PropagationSnapshot;
import dev.reddragon.analytics.model.VolatilityExpansionSnapshot;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.marketdata.model.MarketDataSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Performs contradiction-oriented analysis against the bullish thesis.
 */
public class AdversarialValidationAnalyzer {

    /**
     * Main processing flow.
     */
    public List<AdversarialFinding> process(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            IntradayStructureSnapshot intraday,
            LiquidityTextureSnapshot liquidity,
            PropagationSnapshot propagation,
            FundamentalImpactSnapshot impact,
            VolatilityExpansionSnapshot volatility,
            OptionsFlowSnapshot optionsFlow
    ) {
        Objects.requireNonNull(candidate, "candidate is required");

        List<AdversarialFinding> findings = new ArrayList<>();

        hypeWithoutStructure(candidate, propagation, findings);
        lateNarrativeAfterRepricing(candidate, marketData, propagation, findings);
        optionsChaseWithoutReality(candidate, optionsFlow, findings);
        volatilityWithLiquidityDeterioration(liquidity, volatility, findings);
        materialityWithoutResponse(candidate, propagation, impact, findings);
        sectorSympathyWithoutCatalyst(candidate, propagation, findings);
        strongCatalystIsolated(candidate, propagation, findings);
        vwapReclaimWeakEquilibrium(intraday, marketData, findings);
        highMentionLowCoherence(propagation, findings);
        asymmetryCompressedByExtension(marketData, intraday, findings);

        return List.copyOf(findings);
    }

    private void hypeWithoutStructure(
            TradeCandidate candidate,
            PropagationSnapshot propagation,
            List<AdversarialFinding> findings
    ) {
        if (candidate.structuralRealityScore() < 0.45
                && propagation.mentionVelocityScore() > 0.75) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.HYPE_WITHOUT_STRUCTURE,
                    0.85,
                    "Propagation is accelerating despite weak structural reality."
            ));
        }
    }

    private void lateNarrativeAfterRepricing(
            TradeCandidate candidate,
            MarketDataSnapshot marketData,
            PropagationSnapshot propagation,
            List<AdversarialFinding> findings
    ) {
        if (candidate.earlynessScore() < 0.45
                && marketData.rangePosition() > 0.85
                && propagation.propagationAccelerationScore() > 0.70) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.LATE_NARRATIVE_AFTER_REPRICING,
                    0.90,
                    "Narrative acceleration appears after significant repricing already occurred."
            ));
        }
    }

    private void optionsChaseWithoutReality(
            TradeCandidate candidate,
            OptionsFlowSnapshot optionsFlow,
            List<AdversarialFinding> findings
    ) {
        if (candidate.structuralRealityScore() < 0.50
                && optionsFlow.unusualActivityScore() > 0.80) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.OPTIONS_CHASE_WITHOUT_REALITY,
                    0.80,
                    "Options activity appears speculative relative to structural reality."
            ));
        }
    }

    private void volatilityWithLiquidityDeterioration(
            LiquidityTextureSnapshot liquidity,
            VolatilityExpansionSnapshot volatility,
            List<AdversarialFinding> findings
    ) {
        if (liquidity.liquidityConsistencyScore() < 0.40
                && volatility.realizedVolatilityExpansionScore() > 0.75) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.VOLATILITY_WITH_LIQUIDITY_DETERIORATION,
                    0.88,
                    "Volatility expansion is occurring alongside liquidity deterioration."
            ));
        }
    }

    private void materialityWithoutResponse(
            TradeCandidate candidate,
            PropagationSnapshot propagation,
            FundamentalImpactSnapshot impact,
            List<AdversarialFinding> findings
    ) {
        if (impact.marketCapRelativeImpactScore() > 0.75
                && propagation.crossPlatformExpansionScore() < 0.35
                && candidate.earlynessScore() > 0.70) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.MATERIALITY_WITHOUT_MARKET_RESPONSE,
                    0.65,
                    "Material catalyst exists but broader participation response is weak."
            ));
        }
    }

    private void sectorSympathyWithoutCatalyst(
            TradeCandidate candidate,
            PropagationSnapshot propagation,
            List<AdversarialFinding> findings
    ) {
        if (candidate.structuralRealityScore() < 0.40
                && propagation.sectorSympathyScore() > 0.75) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.SECTOR_SYMPATHY_WITH_WEAK_CATALYST,
                    0.70,
                    "Sector sympathy appears stronger than the underlying catalyst quality."
            ));
        }
    }

    private void strongCatalystIsolated(
            TradeCandidate candidate,
            PropagationSnapshot propagation,
            List<AdversarialFinding> findings
    ) {
        if (candidate.structuralRealityScore() > 0.80
                && propagation.crossPlatformExpansionScore() < 0.25) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.STRONG_CATALYST_ISOLATED_PROPAGATION,
                    0.45,
                    "Strong catalyst exists but propagation remains unusually isolated."
            ));
        }
    }

    private void vwapReclaimWeakEquilibrium(
            IntradayStructureSnapshot intraday,
            MarketDataSnapshot marketData,
            List<AdversarialFinding> findings
    ) {
        if (intraday.vwapReclaimStrength() > 0.75
                && marketData.volatilityStabilityScore() < 0.35) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.VWAP_RECLAIM_WITH_WEAK_EQUILIBRIUM,
                    0.60,
                    "VWAP reclaim appears constructive but broader equilibrium remains unstable."
            ));
        }
    }

    private void highMentionLowCoherence(
            PropagationSnapshot propagation,
            List<AdversarialFinding> findings
    ) {
        if (propagation.mentionVelocityScore() > 0.80
                && propagation.narrativeCoherenceScore() < 0.40) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.HIGH_MENTION_LOW_COHERENCE,
                    0.78,
                    "Mentions are accelerating but narrative coherence remains poor."
            ));
        }
    }

    private void asymmetryCompressedByExtension(
            MarketDataSnapshot marketData,
            IntradayStructureSnapshot intraday,
            List<AdversarialFinding> findings
    ) {
        if (marketData.rangePosition() > 0.90
                && Math.abs(intraday.vwapDistancePercent()) > 0.08) {
            findings.add(new AdversarialFinding(
                    AdversarialFindingType.ASYMMETRY_COMPRESSED_BY_EXTENSION,
                    0.85,
                    "Price extension suggests asymmetry may already be compressed."
            ));
        }
    }
}
