package dev.reddragon.persistence.domains;

import java.time.Instant;

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

@Entity
@Table(name = "market_quote_observation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MarketQuoteObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "last_price", nullable = false)
    private double lastPrice;

    @Column(name = "bid_price", nullable = false)
    private double bidPrice;

    @Column(name = "ask_price", nullable = false)
    private double askPrice;

    @Column(name = "volume", nullable = false)
    private long volume;

    @Column(name = "quality", nullable = false, length = 64)
    private String quality;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** DB-populated insertion timestamp (V13). */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Builder constructor for new rows; generated IDs and audit columns are database-managed. */
    @Builder
    public MarketQuoteObservationEntity(
            String symbol,
            Instant observedAt,
            double lastPrice,
            double bidPrice,
            double askPrice,
            long volume,
            String quality,
            String notes
    ) {
        this(null, symbol, observedAt, lastPrice, bidPrice, askPrice, volume,
                quality, notes, null);
    }

    /** Package-private legacy constructor for tests and migration fixtures. */
    MarketQuoteObservationEntity(
            Long id,
            String symbol,
            Instant observedAt,
            double lastPrice,
            double bidPrice,
            double askPrice,
            long volume,
            String quality,
            String notes
    ) {
        this(id, symbol, observedAt, lastPrice, bidPrice, askPrice, volume,
                quality, notes, null);
    }
}
