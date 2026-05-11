package dev.reddragon.ingestion.sec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Converts a column-oriented {@link SubmissionsResponse} into a flat list
 * of {@link SecFiling} rows, filtered to the form types we care about.
 *
 * <p>The submissions endpoint stores parallel arrays — every list has the
 * same length and index {@code i} in each list refers to the same filing.
 * This class denormalises that shape and drops everything outside our
 * scope so downstream code never has to know about the wire format.
 */
public class SubmissionsFilingExtractor {

    private static final Set<String> FORMS_IN_SCOPE = Set.of(
            "8-K", "4", "SC 13D", "SC 13G", "S-1", "S-3", "424B"
    );

    /**
     * Walks the response and returns one {@link SecFiling} per in-scope
     * filing, ordered from most recent to oldest (i.e., the SEC's order).
     */
    public List<SecFiling> process(SubmissionsResponse response) {
        Objects.requireNonNull(response, "response is required");

        SubmissionsResponse.Recent recent = unpackRecent(response);
        if (recent == null) {
            return List.of();
        }

        int filingCount = recent.accessionNumber().size();
        List<SecFiling> filings = new ArrayList<>(filingCount);
        for (int index = 0; index < filingCount; index++) {
            if (isInScope(recent.form().get(index))) {
                filings.add(buildFiling(response, recent, index));
            }
        }
        return filings;
    }

    private SubmissionsResponse.Recent unpackRecent(SubmissionsResponse response) {
        if (response.filings() == null) {
            return null;
        }
        return response.filings().recent();
    }

    private boolean isInScope(String formType) {
        return formType != null && FORMS_IN_SCOPE.contains(formType.trim());
    }

    private SecFiling buildFiling(SubmissionsResponse response, SubmissionsResponse.Recent recent, int i) {
        return new SecFiling(
                response.cik(),
                response.name(),
                firstTickerOrEmpty(response),
                recent.accessionNumber().get(i),
                recent.form().get(i).trim(),
                parseFilingDate(recent.filingDate().get(i)),
                safeGet(recent.primaryDocument(), i),
                safeGet(recent.primaryDocDescription(), i),
                parseItemCodes(safeGet(recent.items(), i))
        );
    }

    private String firstTickerOrEmpty(SubmissionsResponse response) {
        if (response.tickers() == null || response.tickers().isEmpty()) {
            return "";
        }
        return response.tickers().get(0);
    }

    private LocalDate parseFilingDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private String safeGet(List<String> list, int index) {
        if (list == null || index >= list.size()) {
            return "";
        }
        String value = list.get(index);
        return value == null ? "" : value;
    }

    private List<String> parseItemCodes(String rawItems) {
        if (rawItems == null || rawItems.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rawItems.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
