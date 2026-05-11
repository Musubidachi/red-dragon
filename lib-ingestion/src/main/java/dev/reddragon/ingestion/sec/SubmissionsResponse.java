package dev.reddragon.ingestion.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw deserialisation of the SEC submissions JSON payload.
 *
 * <p>This class only represents the wire format. It does not validate,
 * denormalise, score, or filter. {@link SubmissionsFilingExtractor} performs
 * that work through its {@code process} method.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionsResponse(
        String cik,
        String name,
        @JsonProperty("tickers") List<String> tickers,
        SubmissionsFilings filings
) {
}
