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
@Table(name = "intraday_bar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IntradayBarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

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

    @Column(name = "vwap", nullable = false)
    private double vwap;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Builder constructor for new rows; generated IDs are database-managed. */
    @Builder
    public IntradayBarEntity(
            String symbol,
            Instant startTime,
            double openPrice,
            double highPrice,
            double lowPrice,
            double closePrice,
            long volume,
            double vwap,
            Instant createdAt
    ) {
        this(null, symbol, startTime, openPrice, highPrice, lowPrice, closePrice, volume, vwap, createdAt);
    }
}
