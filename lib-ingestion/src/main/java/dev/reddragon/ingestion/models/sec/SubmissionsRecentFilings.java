package dev.reddragon.ingestion.models.sec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Column-oriented SEC recent filing arrays.
 *
 * <p>Each list contains one entry per filing, in chronological order. The
 * SEC payload is column-oriented so the lists are guaranteed to be
 * parallel; the extractor flattens them into one {@link SecFiling} per
 * row.
 *
 * <p><b>Beyond the original six columns:</b> the SEC also emits
 * {@code acceptanceDateTime} (the wall-clock instant the document
 * actually arrived at EDGAR, with seconds precision) and the two XBRL
 * flags. Parsing {@code acceptanceDateTime} fixes the earlyness
 * UTC-midnight tier-jump (lib-ingestion REVIEW.md Finding #18) — without
 * it the candidate scoring anchors at {@code filingDate.atStartOfDay()},
 * which is wrong by up to a full day for filings made during US market
 * hours. {@code isXBRL} / {@code isInlineXBRL} (Finding #19) are parsed
 * so future code can filter on machine-readable financial filings.
 *
 * <p>All four new columns are nullable / optional — older fixtures or
 * stub responses can omit them and the extractor falls back to the
 * existing {@code filingDate} behaviour.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmissionsRecentFilings(
        List<String> accessionNumber,
        List<String> filingDate,
        List<String> form,
        List<String> primaryDocument,
        List<String> primaryDocDescription,
        List<String> items,
        @JsonProperty("acceptanceDateTime") List<String> acceptanceDateTime,
        @JsonProperty("isXBRL") List<Integer> isXbrl,
        @JsonProperty("isInlineXBRL") List<Integer> isInlineXbrl
) {

    /**
     * Backwards-compatible six-arg constructor. Tests and stub callers
     * that don't supply the new columns get empty lists, and the
     * extractor falls back to the legacy {@code filingDate}-only path.
     */
    public SubmissionsRecentFilings(
            List<String> accessionNumber,
            List<String> filingDate,
            List<String> form,
            List<String> primaryDocument,
            List<String> primaryDocDescription,
            List<String> items
    ) {
        this(accessionNumber, filingDate, form, primaryDocument,
                primaryDocDescription, items,
                List.of(), List.of(), List.of());
    }
}
