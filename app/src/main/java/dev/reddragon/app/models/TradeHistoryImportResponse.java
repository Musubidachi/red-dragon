package dev.reddragon.app.models;

import java.util.List;

/**
 * Import summary with normalized rows and non-fatal parse warnings.
 */
public record TradeHistoryImportResponse(
        int totalRows,
        int importedRows,
        List<String> warnings,
        List<NormalizedTradeRecord> trades
) {
}
