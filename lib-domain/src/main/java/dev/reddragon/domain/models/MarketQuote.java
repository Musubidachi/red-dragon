package dev.reddragon.domain.models;

import java.time.Instant;
import java.util.List;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class MarketQuote {
    String symbol;
    Instant observedAt;
    double lastPrice;
    double bidPrice;
    double askPrice;
    long volume;
    MarketDataQuality quality;
    List<String> notes;

    public MarketQuote(
            String symbol,
            Instant observedAt,
            double lastPrice,
            double bidPrice,
            double askPrice,
            long volume,
            MarketDataQuality quality,
            List<String> notes
    ) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (lastPrice < 0 || bidPrice < 0 || askPrice < 0) {
            throw new IllegalArgumentException("prices must be non-negative");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("volume must be non-negative");
        }
        this.symbol = symbol.trim().toUpperCase();
        this.observedAt = observedAt == null ? Instant.now() : observedAt;
        this.lastPrice = lastPrice;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.volume = volume;
        this.quality = quality == null ? MarketDataQuality.COMPLETE : quality;
        this.notes = List.copyOf(notes == null ? List.of() : notes);
    }

    public static MarketQuote unavailable(String symbol, String note) {
        String safeSymbol = symbol == null || symbol.isBlank() ? "UNKNOWN" : symbol;
        String safeNote = note == null || note.isBlank() ? "Quote unavailable." : note;
        return new MarketQuote(
                safeSymbol,
                Instant.now(),
                0.0,
                0.0,
                0.0,
                0L,
                MarketDataQuality.EMPTY_BARS,
                List.of(safeNote)
        );
    }

    public boolean available() {
        return quality == MarketDataQuality.COMPLETE && lastPrice > 0.0;
    }

    public double spread() {
        if (bidPrice <= 0.0 || askPrice <= 0.0) {
            return 0.0;
        }
        return askPrice - bidPrice;
    }
}
