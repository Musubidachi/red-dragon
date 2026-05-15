package dev.reddragon.ingestion.models;

/**
 * Classifies the root catalyst behind a trade candidate.
 *
 * <p>Catalysts drive the disequilibrium event; the type helps the analytics layer
 * calibrate credibility, propagation speed, and regime compatibility expectations.
 */
public enum CandidateCatalystType {
    GOVERNMENT_GRANT,
    POLICY_CHANGE,
    CONTRACT,
    SUPPLY_CONSTRAINT,
    SECTOR_INCENTIVE,
    LIQUIDITY_SHIFT,
    STRUCTURAL_DEMAND_CHANGE,
    FILING_EVENT,
    NEWS_EVENT,
    SCANNER_EVENT,
    MANUAL_THESIS;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case GOVERNMENT_GRANT        -> "Government Grant";
            case POLICY_CHANGE           -> "Policy Change";
            case CONTRACT                -> "Contract";
            case SUPPLY_CONSTRAINT       -> "Supply Constraint";
            case SECTOR_INCENTIVE        -> "Sector Incentive";
            case LIQUIDITY_SHIFT         -> "Liquidity Shift";
            case STRUCTURAL_DEMAND_CHANGE -> "Structural Demand Change";
            case FILING_EVENT            -> "Filing Event";
            case NEWS_EVENT              -> "News Event";
            case SCANNER_EVENT           -> "Scanner Event";
            case MANUAL_THESIS           -> "Manual Thesis";
        };
    }
}
