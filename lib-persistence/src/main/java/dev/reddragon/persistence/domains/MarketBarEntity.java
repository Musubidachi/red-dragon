package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_bar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MarketBarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "bar_date", nullable = false)
    private LocalDate barDate;

    @Column(name = "open_price", nullable = false)
    private double openPrice;

    @Column(name = "high_price", nullable = false)
    private double highPrice;

    @Column(name = "low_price", nullable = false)
    private double lowPrice;

    @Column(name = "close_price", nullable = false)
    private double closePrice;

    @Column(name = "volume", nullable = false)
    private long volume;

    /**
     * Wall-clock instant the row landed in the database. Populated by the
     * column default in V11 ({@code default current_timestamp}). Provides
     * provenance separate from {@link #barDate} (the trading-day the bar
     * represents).
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Builder constructor for new rows; generated IDs and audit columns are database-managed. */
    @Builder
    public MarketBarEntity(
            String symbol,
            LocalDate barDate,
            double openPrice,
            double highPrice,
            double lowPrice,
            double closePrice,
            long volume
    ) {
        this(null, symbol, barDate, openPrice, highPrice, lowPrice, closePrice, volume, null);
    }
}
