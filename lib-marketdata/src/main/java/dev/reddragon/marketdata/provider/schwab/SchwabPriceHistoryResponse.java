package dev.reddragon.marketdata.provider.schwab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level Jackson DTO for the Schwab /pricehistory response.
 *
 * <p>Only the {@code candles} array is consumed; the rest of the response is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SchwabPriceHistoryResponse(List<SchwabCandle> candles) {
}
