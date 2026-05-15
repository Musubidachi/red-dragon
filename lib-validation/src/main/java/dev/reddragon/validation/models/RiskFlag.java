package dev.reddragon.validation.models;

/**
 * Additional qualitative risks associated with a validation outcome.
 */
public enum RiskFlag {
    LATE_ENTRY_RISK,
    DATA_QUALITY_RISK,
    VOLATILITY_RISK,
    ASYMMETRY_COMPRESSION_RISK,
    REGIME_RISK,
    LIQUIDITY_RISK,
    REFLEXIVITY_SATURATION_RISK;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case LATE_ENTRY_RISK              -> "Late Entry Risk";
            case DATA_QUALITY_RISK            -> "Data Quality Risk";
            case VOLATILITY_RISK              -> "Volatility Risk";
            case ASYMMETRY_COMPRESSION_RISK   -> "Asymmetry Compression Risk";
            case REGIME_RISK                  -> "Regime Risk";
            case LIQUIDITY_RISK               -> "Liquidity Risk";
            case REFLEXIVITY_SATURATION_RISK  -> "Reflexivity Saturation Risk";
        };
    }

    /** Short description of what this risk flag means for the trade setup. */
    public String description() {
        return switch (this) {
            case LATE_ENTRY_RISK             -> "Entry point may be past the optimal asymmetry window; disequilibrium may already be partially resolved.";
            case DATA_QUALITY_RISK           -> "Insufficient or unreliable market data; validation confidence is reduced.";
            case VOLATILITY_RISK             -> "Elevated volatility may compress asymmetry and increase slippage risk.";
            case ASYMMETRY_COMPRESSION_RISK  -> "Risk/reward asymmetry is narrower than ideal; potential upside is limited relative to downside.";
            case REGIME_RISK                 -> "Market regime is hostile or mixed; structural tailwinds are absent.";
            case LIQUIDITY_RISK              -> "Thin liquidity may prevent effective entry or exit at modeled prices.";
            case REFLEXIVITY_SATURATION_RISK -> "Reflexive dynamics may be overextended; late-stage momentum rather than early disequilibrium.";
        };
    }
}
