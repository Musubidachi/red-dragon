package dev.reddragon.ingestion.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Column-oriented SEC recent filing arrays.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionsRecentFilings(
        List<String> accessionNumber,
        List<String> filingDate,
        List<String> form,
        List<String> primaryDocument,
        List<String> primaryDocDescription,
        List<String> items
) {
}
