package dev.reddragon.persistence.services.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.TradeHistoryRecordEntity;

public interface TradeHistoryRecordRepository extends JpaRepository<TradeHistoryRecordEntity, Long> {

    List<TradeHistoryRecordEntity> findByImportBatchIdOrderByTradeTimestampAsc(Long importBatchId);

    List<TradeHistoryRecordEntity> findByTickerAndTradeTimestampAfterOrderByTradeTimestampDesc(
            String ticker,
            Instant since
    );

    List<TradeHistoryRecordEntity> findByTradeTimestampAfterOrderByTradeTimestampDesc(Instant since);

    List<TradeHistoryRecordEntity> findTop100ByOrderByTradeTimestampDesc();
}
