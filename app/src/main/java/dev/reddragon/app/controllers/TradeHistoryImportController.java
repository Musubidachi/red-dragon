package dev.reddragon.app.controllers;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.NormalizedTradeRecord;
import dev.reddragon.app.models.TradeHistoryImportRequest;
import dev.reddragon.app.models.TradeHistoryImportResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/history")
public class TradeHistoryImportController {

    @PostMapping("/import")
    public ResponseEntity<TradeHistoryImportResponse> importCsv(
            @Valid @RequestBody TradeHistoryImportRequest request
    ) {
        List<String> warnings = new ArrayList<>();
        List<NormalizedTradeRecord> trades = new ArrayList<>();

        String[] lines = request.csv().split("\\r?\\n");
        if (lines.length <= 1) {
            return ResponseEntity.ok(new TradeHistoryImportResponse(0, 0, List.of("No data rows found."), List.of()));
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

        return ResponseEntity.ok(new TradeHistoryImportResponse(totalRows, trades.size(), warnings, trades));
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
