package dev.reddragon.ingestion.models;

/**
 * Identifies the ingestion channel that produced a trade candidate.
 */
public enum SourceType {
    MANUAL,
    SEC_EDGAR,
    NEWS_RSS,
    SCANNER,
    MACRO_FEED;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case MANUAL     -> "Manual";
            case SEC_EDGAR  -> "SEC EDGAR";
            case NEWS_RSS   -> "News RSS";
            case SCANNER    -> "Scanner";
            case MACRO_FEED -> "Macro Feed";
        };
    }
}
