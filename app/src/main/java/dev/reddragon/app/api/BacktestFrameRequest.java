package dev.reddragon.app.api;

import lombok.Data;

import java.util.List;

/**
 * One historical candidate + its bars for a backtest run.
 * Scores are supplied by the caller — they represent the analyst's assessment
 * at the time the candidate was observed, not the current assessment.
 */
@Data
public class BacktestFrameRequest {

    private String symbol;
    private String companyName;
    private String catalystType;
    private String headline;
    private String summary;

    private double structuralRealityScore;
    private double materialSignificanceScore;
    private double earlynessScore;
    private double reflexivityPotentialScore;

    private List<BacktestBarRequest> bars;
}
