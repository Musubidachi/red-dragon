package dev.reddragon.analytics.service;

import dev.reddragon.analytics.model.MarketStateSignal;
import dev.reddragon.analytics.model.RegimeLabel;
import dev.reddragon.marketdata.model.MarketIntradayStructureSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rule-based classifier for environment suitability.
 */
public class MarketStateClassifier {

    /**
     * Main processing flow.
     */
    public MarketStateSignal process(MarketIntradayStructureSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot is required");

        List<String> notes = new ArrayList<>();

        double rotational = snapshot.rotationalQualityScore();
        double persistence = snapshot.directionalPersistenceScore();
        double trend = snapshot.intradayTrendStrength();
        double reclaim = snapshot.vwapReclaimStrength();

        RegimeLabel regime = RegimeLabel.MIXED;
        double confidence = 0.5;

        if (rotational >= 0.65 && persistence < 0.55 && trend < 0.60) {
            regime = RegimeLabel.SUPPORTIVE_ROTATIONAL;
            confidence = (rotational * 0.55) + ((1.0 - persistence) * 0.20) + ((1.0 - trend) * 0.15) + (reclaim * 0.10);
            notes.add("Rotational quality is elevated while trend persistence is contained.");
        } else if (persistence >= 0.70 && trend >= 0.65) {
            regime = RegimeLabel.SUPPORTIVE_TREND;
            confidence = (persistence * 0.45) + (trend * 0.35) + ((1.0 - rotational) * 0.10) + (reclaim * 0.10);
            notes.add("Directional persistence and intraday trend are both elevated.");
        } else if (reclaim < 0.30 && Math.abs(snapshot.vwapDistancePercent()) > 2.25) {
            regime = RegimeLabel.HOSTILE_VOLATILITY;
            confidence = ((1.0 - reclaim) * 0.50) + Math.min(1.0, Math.abs(snapshot.vwapDistancePercent()) / 4.0) * 0.35 + (trend * 0.15);
            notes.add("VWAP reclaim quality is weak with extended displacement.");
        } else if (rotational < 0.30 && persistence < 0.35 && trend < 0.35) {
            regime = RegimeLabel.HOSTILE_LIQUIDITY;
            confidence = ((1.0 - rotational) * 0.40) + ((1.0 - persistence) * 0.30) + ((1.0 - trend) * 0.30);
            notes.add("Low rotational and directional quality suggest degraded participation.");
        } else {
            notes.add("Signals are mixed; no dominant market-state edge detected.");
            confidence = Math.max(Math.max(rotational, persistence), Math.max(trend, reclaim));
        }

        double restorationProbability = estimateRestorationProbability(snapshot, regime);
        boolean deploy = regime.isSupportive() && restorationProbability >= 0.55 && confidence >= 0.55;

        notes.add("Estimated equilibrium restoration probability: " + String.format("%.2f", restorationProbability));
        notes.add(deploy
                ? "Deployment supported under current regime assumptions."
                : "Deployment should remain selective or reduced.");

        return new MarketStateSignal(regime, confidence, restorationProbability, deploy, notes);
    }

    private double estimateRestorationProbability(MarketIntradayStructureSnapshot snapshot, RegimeLabel regime) {
        double base = switch (regime) {
            case SUPPORTIVE_ROTATIONAL -> 0.68;
            case SUPPORTIVE_TREND -> 0.58;
            case MIXED -> 0.50;
            case HOSTILE_VOLATILITY -> 0.36;
            case HOSTILE_LIQUIDITY -> 0.32;
        };

        double vwapPenalty = Math.min(0.20, Math.abs(snapshot.vwapDistancePercent()) * 0.04);
        double reclaimBoost = snapshot.vwapReclaimStrength() * 0.12;
        double rotationalBoost = snapshot.rotationalQualityScore() * 0.08;

        double probability = base - vwapPenalty + reclaimBoost + rotationalBoost;
        return Math.max(0.0, Math.min(1.0, probability));
    }
}
