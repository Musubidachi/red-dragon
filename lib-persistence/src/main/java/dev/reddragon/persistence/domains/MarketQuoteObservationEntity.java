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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_quote_observation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    @Column(name = "notes", length = 4000)
    private String notes;
}
