package dev.reddragon.persistence.domains;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_history_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TradeHistoryRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "import_batch_id", nullable = false)
    private Long importBatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id", insertable = false, updatable = false)
    private TradeHistoryImportBatchEntity importBatch;

    @Column(name = "trade_timestamp", nullable = false)
    private Instant tradeTimestamp;

    @Column(name = "ticker", nullable = false, length = 16)
    private String ticker;

    @Column(name = "side", nullable = false, length = 32)
    private String side;

    @Column(name = "quantity", nullable = false)
    private double quantity;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "realized_pnl")
    private Double realizedPnl;

    @Column(name = "account", length = 128)
    private String account;

    @Column(name = "strategy_type", length = 64)
    private String strategyType;

    @Column(name = "market_state", length = 64)
    private String marketState;

    /** DB-populated insertion timestamp (V13). */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Backwards-compatible pre-V13 constructor. */
    public TradeHistoryRecordEntity(
            Long id,
            Long importBatchId,
            TradeHistoryImportBatchEntity importBatch,
            Instant tradeTimestamp,
            String ticker,
            String side,
            double quantity,
            double price,
            Double realizedPnl,
            String account,
            String strategyType,
            String marketState
    ) {
        this(id, importBatchId, importBatch, tradeTimestamp, ticker, side,
                quantity, price, realizedPnl, account, strategyType, marketState,
                null);
    }
}
