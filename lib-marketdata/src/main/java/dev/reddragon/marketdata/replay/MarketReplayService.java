package dev.reddragon.marketdata.replay;

import java.util.List;
import java.util.Objects;

/**
 * Iterates replay frames for deterministic historical playback.
 */
public class MarketReplayService {

    /**
     * Main processing flow.
     */
    public List<MarketReplayFrame> process(List<MarketReplayFrame> frames) {
        Objects.requireNonNull(frames, "frames are required");
        return List.copyOf(frames);
    }
}
