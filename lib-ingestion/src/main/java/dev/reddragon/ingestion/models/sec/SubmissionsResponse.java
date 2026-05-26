package dev.reddragon.ingestion.models.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw deserialisation of the SEC submissions JSON payload.
 *
 * <p>This class only represents the wire format. It does not validate,
 * denormalise, score, or filter. {@link SubmissionsFilingExtractor} performs
 * that work through its {@code process} method.
 *
 * <p>{@code tickers} and {@code exchanges} are parallel top-level arrays:
 * {@code exchanges[i]} is the exchange name for {@code tickers[i]} (the SEC
 * pairs them by index). The exchange list lets the extractor filter OTC /
 * Pink tickers and disambiguate share classes; if {@code exchanges} is
 * missing or shorter than {@code tickers}, the unmatched tickers are
 * treated as "unknown exchange" and kept (defensive — we don't drop a
 * ticker for which we lack OTC evidence).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionsResponse(
        String cik,
        String name,
        @JsonProperty("tickers") List<String> tickers,
        @JsonProperty("exchanges") List<String> exchanges,
        SubmissionsFilings filings
) {

    /**
     * Compact constructor. Defends against {@code null} list fields from
     * either Jackson (when the SEC omits one of the arrays) or test fixtures
     * by substituting empty lists so callers can always iterate safely.
     */
    public SubmissionsResponse {
        if (tickers == null) {
            tickers = List.of();
        }
        if (exchanges == null) {
            exchanges = List.of();
        }
    }

    /**
     * Convenience constructor that preserves the pre-exchanges API for
     * callers that don't supply exchange data (typically older tests).
     * Delegates to the canonical constructor with an empty exchanges list.
     */
    public SubmissionsResponse(
            String cik,
            String name,
            List<String> tickers,
            SubmissionsFilings filings
    ) {
        this(cik, name, tickers, List.of(), filings);
    }
}
