package dev.reddragon.validation.model;

/**
 * Final validation decision for a trade candidate.
 */
public enum Verdict {
    /** Candidate is strong enough to appear on the trader review surface. */
    PASS,

    /** Candidate has promise, but needs confirmation or has moderate defects. */
    WATCH,

    /** Candidate fails one or more required validation rules. */
    REJECT
}
