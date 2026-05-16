package dev.reddragon.ingestion.services.sec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsRecentFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;

/**
 * Converts the SEC column-oriented payload into flat filing rows.
 */
public class SubmissionsFilingExtractor {

    private static final Set<String> FORMS_IN_SCOPE = Set.of(
            "8-K",
            "4",
            "SC 13D",
            "SC 13G",
            "S-1",
            "S-3",
            "424B"
    );

    /**
     * Main processing flow.
     */
    public List<SecFiling> process(SubmissionsResponse response) {
        Objects.requireNonNull(response, "response is required");

        SubmissionsRecentFilings recentFilings = recentFilings(response);
        if (recentFilings == null) {
            return List.of();
        }

        return buildFilings(response, recentFilings);
    }

    private SubmissionsRecentFilings recentFilings(SubmissionsResponse response) {
        if (response.filings() == null) {
            return null;
        }
        return response.filings().recent();
    }

    private List<SecFiling> buildFilings(
            SubmissionsResponse response,
            SubmissionsRecentFilings recentFilings
    ) {
        int filingCount = recentFilings.accessionNumber().size();
        List<SecFiling> filings = new ArrayList<>(filingCount);

        for (int index = 0; index < filingCount; index++) {
            String formType = recentFilings.form().get(index);
            if (isInScope(formType)) {
                filings.add(buildFiling(response, recentFilings, index));
            }
        }

        return filings;
    }

    private boolean isInScope(String formType) {
        return formType != null && FORMS_IN_SCOPE.contains(formType.trim());
    }

    private SecFiling buildFiling(
            SubmissionsResponse response,
            SubmissionsRecentFilings recentFilings,
            int index
    ) {
        return new SecFiling(
                response.cik(),
                response.name(),
                ticker(response),
                recentFilings.accessionNumber().get(index),
                recentFilings.form().get(index).trim(),
                filingDate(recentFilings.filingDate().get(index)),
                safeValue(recentFilings.primaryDocument(), index),
                safeValue(recentFilings.primaryDocDescription(), index),
                itemCodes(safeValue(recentFilings.items(), index))
        );
    }

    private String ticker(SubmissionsResponse response) {
        if (response.tickers() == null || response.tickers().isEmpty()) {
            return "";
        }
        return response.tickers().get(0);
    }

    private LocalDate filingDate(String filingDate) {
        if (filingDate == null || filingDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(filingDate);
    }

    private String safeValue(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return "";
        }

        String value = values.get(index);
        return value == null ? "" : value;
    }

    private List<String> itemCodes(String rawItems) {
        if (rawItems == null || rawItems.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(rawItems.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
