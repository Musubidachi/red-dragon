package dev.reddragon.app.controllers;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.NormalizedTradeRecord;
import dev.reddragon.app.models.TradeHistoryImportRequest;
import dev.reddragon.app.models.TradeHistoryImportResponse;
import dev.reddragon.persistence.domains.TradeHistoryImportBatchEntity;
import dev.reddragon.persistence.domains.TradeHistoryRecordEntity;
import dev.reddragon.persistence.services.repositories.TradeHistoryImportBatchRepository;
import dev.reddragon.persistence.services.repositories.TradeHistoryRecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class TradeHistoryImportController {

    private final TradeHistoryImportBatchRepository batchRepository;
    private final TradeHistoryRecordRepository recordRepository;

    @PostMapping("/import")
    public ResponseEntity<TradeHistoryImportResponse> importCsv(
            @Valid @RequestBody TradeHistoryImportRequest request
    ) {
        List<String> warnings = new ArrayList<>();
        List<NormalizedTradeRecord> trades = new ArrayList<>();

        String[] lines = request.csv().split("\\r?\\n");
        if (lines.length <= 1) {
            warnings.add("No data rows found.");
            persistImport(0, warnings, List.of());
            return ResponseEntity.ok(new TradeHistoryImportResponse(0, 0, warnings, List.of()));
        }

        int totalRows = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            totalRows++;
            try {
                String[] cols = line.split(",", -1);
                if (cols.length < 7) {
                    warnings.add("Row " + (i + 1) + " skipped: expected at least 7 columns.");
                    continue;
                }

                Instant timestamp = parseTimestamp(cols[0], i, warnings);
                if (timestamp == null) {
                    continue;
                }

                String ticker = cols[1].trim().toUpperCase(Locale.ROOT);
                String side = cols[2].trim().toUpperCase(Locale.ROOT);
                double quantity = parseDouble(cols[3], "quantity", i, warnings);
                double price = parseDouble(cols[4], "price", i, warnings);
                Double realizedPnl = parseOptionalDouble(cols[5], "realizedPnL", i, warnings);
                String account = cols[6].trim();

                String strategyType = inferStrategyType(side, quantity, realizedPnl);
                String marketState = inferMarketState(side, realizedPnl);

                trades.add(new NormalizedTradeRecord(
                        timestamp,
                        ticker,
                        side,
                        quantity,
                        price,
                        realizedPnl,
                        account,
                        strategyType,
                        marketState
                ));
            } catch (RuntimeException ex) {
                warnings.add("Row " + (i + 1) + " skipped: " + ex.getMessage());
            }
        }

        persistImport(totalRows, warnings, trades);

        return ResponseEntity.ok(new TradeHistoryImportResponse(totalRows, trades.size(), warnings, trades));
    }

    @GetMapping("/imports")
    public List<TradeHistoryImportBatchEntity> imports() {
        return batchRepository.findTop25ByOrderByImportedAtDesc();
    }

    @GetMapping("/imports/{batchId}")
    public ResponseEntity<Map<String, Object>> importDetail(@PathVariable Long batchId) {
        return batchRepository.findById(batchId)
                .map(batch -> ResponseEntity.ok(importDetail(batch)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/trades")
    public List<TradeHistoryRecordEntity> trades(
            @RequestParam(required = false) String ticker,
            @RequestParam(defaultValue = "720") int lookbackHours
    ) {
        if (ticker != null && !ticker.isBlank()) {
            return recordRepository.findByTickerAndTradeTimestampAfterOrderByTradeTimestampDesc(
                    ticker.trim().toUpperCase(),
                    Instant.now().minusSeconds(Math.max(1, lookbackHours) * 3600L)
            );
        }
        if (lookbackHours > 0) {
            return recordRepository.findByTradeTimestampAfterOrderByTradeTimestampDesc(
                    Instant.now().minusSeconds(lookbackHours * 3600L)
            );
        }
        return recordRepository.findTop100ByOrderByTradeTimestampDesc();
    }

    private Map<String, Object> importDetail(TradeHistoryImportBatchEntity batch) {
        return Map.of(
                "batch", batch,
                "records", recordRepository.findByImportBatchIdOrderByTradeTimestampAsc(batch.getId())
        );
    }

    private void persistImport(
            int totalRows,
            List<String> warnings,
            List<NormalizedTradeRecord> trades
    ) {
        TradeHistoryImportBatchEntity batch = batchRepository.save(new TradeHistoryImportBatchEntity(
                null,
                Instant.now(),
                totalRows,
                trades.size(),
                String.join("\n", warnings)
        ));
        recordRepository.saveAll(trades.stream()
                .map(trade -> toEntity(batch.getId(), trade))
                .toList());
    }

    private TradeHistoryRecordEntity toEntity(Long batchId, NormalizedTradeRecord trade) {
        return new TradeHistoryRecordEntity(
                null,
                batchId,
                null,
                trade.timestamp(),
                trade.ticker(),
                trade.side(),
                trade.quantity(),
                trade.price(),
                trade.realizedPnl(),
                trade.account(),
                trade.strategyType(),
                trade.marketState()
        );
    }

    private Instant parseTimestamp(String value, int row, List<String> warnings) {
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            warnings.add("Row " + (row + 1) + " skipped: invalid timestamp '" + value.trim() + "'.");
            return null;
        }
    }

    private double parseDouble(String value, String name, int row, List<String> warnings) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            warnings.add("Row " + (row + 1) + " skipped: invalid " + name + " '" + value.trim() + "'.");
            throw ex;
        }
    }

    private Double parseOptionalDouble(String value, String name, int row, List<String> warnings) {
        String raw = value.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            warnings.add("Row " + (row + 1) + ": invalid " + name + " '" + raw + "', set to null.");
            return null;
        }
    }

    private String inferStrategyType(String side, double quantity, Double realizedPnl) {
        if ("SELL".equals(side) && realizedPnl != null && realizedPnl > 0) {
            return "equilibrium_recycle";
        }
        if (quantity >= 1_000) {
            return "selective_aggression";
        }
        return "equilibrium";
    }

    private String inferMarketState(String side, Double realizedPnl) {
        if (realizedPnl == null) {
            return "unknown";
        }
        if (realizedPnl > 0 && "SELL".equals(side)) {
            return "rotational";
        }
        if (realizedPnl < 0 && "BUY".equals(side)) {
            return "directional_expansion";
        }
        return "mixed";
    }
}
