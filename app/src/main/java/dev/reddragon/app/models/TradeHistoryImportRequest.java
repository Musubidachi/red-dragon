package dev.reddragon.app.models;

import jakarta.validation.constraints.NotBlank;

/**
 * Raw CSV payload request for one-shot trade history normalization.
 */
public record TradeHistoryImportRequest(
        @NotBlank(message = "csv is required")
        String csv
) {
}
