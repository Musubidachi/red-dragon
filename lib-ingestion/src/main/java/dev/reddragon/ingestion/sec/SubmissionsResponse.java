package dev.reddragon.ingestion.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw deserialisation of the SEC submissions JSON payload.
 *
 * <p>The SEC stores filings as parallel arrays (column-oriented), where
 * index {@code i} of every array refers to the same filing. We map only
 * the subset of fields the rest of the pipeline needs.
 *
 * <p>This record is intentionally dumb. It does not validate, denormalise,
 * or score. {@link SubmissionsFilingExtractor} does that.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionsResponse(
        String cik,
        String name,
        @JsonProperty("tickers") List<String> tickers,
        Filings filings
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Filings(Recent recent) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recent(
            List<String> accessionNumber,
            List<String> filingDate,
            List<String> form,
            List<String> primaryDocument,
            List<String> primaryDocDescription,
            List<String> items
    ) {}
}
