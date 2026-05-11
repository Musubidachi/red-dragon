package dev.reddragon.ingestion.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Container for the SEC recent filings payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionsFilings(
        SubmissionsRecentFilings recent
) {
}
