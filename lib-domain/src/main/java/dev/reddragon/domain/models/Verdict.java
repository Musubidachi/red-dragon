package dev.reddragon.domain.models;

/**
 * Final validation decision for a trade candidate.
 */
public enum Verdict {
    /** Candidate is strong enough to appear on the trader review surface. */
    PASS,

    /** Candidate has promise, but needs confirmation or has moderate defects. */
    WATCH,

    /** Candidate fails one or more required validation rules. */
    REJECT;

    /** Human-readable label suitable for display in review surfaces and reports. */
    public String displayName() {
        return switch (this) {
            case PASS   -> "Pass";
            case WATCH  -> "Watch";
            case REJECT -> "Reject";
        };
    }

    /** Returns {@code true} if this verdict warrants trader attention (PASS or WATCH). */
    public boolean isReviewable() {
        return this == PASS || this == WATCH;
    }
}
