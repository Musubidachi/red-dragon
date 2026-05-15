package dev.reddragon.app.models;

import lombok.Data;

import java.util.List;

/**
 * Top-level request body for POST /api/backtest.
 */
@Data
public class BacktestRequest {

    private String strategyName;
    private List<BacktestFrameRequest> frames;

    /**
     * Validates the request and throws {@link IllegalArgumentException} with a
     * descriptive message if required fields are missing or the frame list is empty.
     */
    public void validate() {
        if (strategyName == null || strategyName.isBlank()) {
            throw new IllegalArgumentException("strategyName must not be blank");
        }
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("frames must not be empty; supply at least one BacktestFrameRequest");
        }
        for (int i = 0; i < frames.size(); i++) {
            BacktestFrameRequest frame = frames.get(i);
            if (frame == null) {
                throw new IllegalArgumentException("frames[" + i + "] must not be null");
            }
            if (frame.getSymbol() == null || frame.getSymbol().isBlank()) {
                throw new IllegalArgumentException("frames[" + i + "].symbol must not be blank");
            }
        }
    }
}
