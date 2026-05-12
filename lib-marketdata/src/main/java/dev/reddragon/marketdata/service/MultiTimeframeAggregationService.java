package dev.reddragon.marketdata.service;

import dev.reddragon.marketdata.model.IntradayBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates lower timeframe bars into higher timeframe structures.
 */
public class MultiTimeframeAggregationService {

    /**
     * Main processing flow.
     */
    public List<IntradayBar> process(
            List<IntradayBar> bars,
            int aggregationSize
    ) {
        Objects.requireNonNull(bars, "bars are required");

        if (aggregationSize <= 0) {
            throw new IllegalArgumentException("aggregationSize must be positive");
        }

        if (bars.isEmpty()) {
            return List.of();
        }

        List<IntradayBar> aggregated = new ArrayList<>();

        for (int index = 0; index < bars.size(); index += aggregationSize) {
            int end = Math.min(index + aggregationSize, bars.size());
            aggregated.add(aggregate(bars.subList(index, end)));
        }

        return List.copyOf(aggregated);
    }

    private IntradayBar aggregate(List<IntradayBar> bars) {
        IntradayBar first = bars.get(0);
        IntradayBar last = bars.get(bars.size() - 1);

        double high = first.high();
        double low = first.low();
        long volume = 0L;
        double vwapWeighted = 0.0;

        for (IntradayBar bar : bars) {
            high = Math.max(high, bar.high());
            low = Math.min(low, bar.low());
            volume += bar.volume();
            vwapWeighted += bar.vwap() * bar.volume();
        }

        double vwap = volume == 0L ? 0.0 : vwapWeighted / volume;

        return new IntradayBar(
                first.symbol(),
                first.startTime(),
                first.open(),
                high,
                low,
                last.close(),
                volume,
                vwap
        );
    }
}
