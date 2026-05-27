package dev.reddragon.analytics.services;

import java.util.List;

/**
 * Immutable score plus scorer-owned narrative notes.
 */
public final class ScoreResult {

    private final double score;
    private final List<String> notes;

    public ScoreResult(double score, List<String> notes) {
        this.score = score;
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }

    public double score() {
        return score;
    }

    public List<String> notes() {
        return notes;
    }
}
