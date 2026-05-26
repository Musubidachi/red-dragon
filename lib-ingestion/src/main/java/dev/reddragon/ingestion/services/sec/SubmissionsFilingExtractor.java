package dev.reddragon.ingestion.services.sec;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsRecentFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;

/**
 * Converts the SEC column-oriented payload into flat filing rows.
 *
 * <p>Two things this class does that the bare deserialisation doesn't:
 * <ul>
 *   <li><b>Ticker selection.</b> An issuer can have multiple tickers
 *       (different share classes) and some of those tickers can be OTC /
 *       Pink listings the framework doesn't want to act on. The
 *       {@code selectTicker} helper pairs {@code tickers[i]} with
 *       {@code exchanges[i]}, filters OTC/Pink listings, and emits the
 *       first remaining ticker (which preserves the SEC's primary-class-
 *       first ordering). Filings whose issuer has no usable ticker after
 *       filtering are dropped — see lib-ingestion REVIEW.md Findings #14
 *       and #17.</li>
 *   <li><b>Item-code splitting.</b> The {@code items} column is a single
 *       comma-separated string per filing; we explode it into a list.</li>
 * </ul>
 */
@Slf4j
public class SubmissionsFilingExtractor {

    /**
     * Canonical form codes the extractor keeps. Variant matching
     * ({@code /A} amendments, {@code 424B1..424B5} subforms) is layered
     * on top in {@link #isInScope(String)} — see lib-ingestion REVIEW.md
     * Finding #24 for what bare {@code Set.contains} silently dropped.
     */
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
     * SEC 424B prospectus subforms. {@code 424B} alone is rare; the real
     * payloads carry {@code 424B1..424B5}. Listed explicitly so a future
     * subform addition is a one-line patch rather than a regex change.
     */
    private static final Set<String> FOUR_TWO_FOUR_B_SUBFORMS = Set.of(
            "424B1", "424B2", "424B3", "424B4", "424B5"
    );

    /**
     * Main processing flow. Returns one {@link SecFiling} per in-scope
     * row in the submissions response that resolves to a usable ticker.
     * Rows for out-of-scope forms or unresolvable tickers are dropped.
     */
    public List<SecFiling> process(SubmissionsResponse response) {
        Objects.requireNonNull(response, "response is required");

        SubmissionsRecentFilings recentFilings = recentFilings(response);
        if (recentFilings == null) {
            return List.of();
        }

        Optional<String> selectedTicker = selectTicker(response);
        if (selectedTicker.isEmpty()) {
            log.warn("Dropping all filings for cik={} ({}): no usable ticker after OTC filtering. tickers={} exchanges={}",
                    response.cik(), response.name(),
                    response.tickers(), response.exchanges());
            return List.of();
        }

        return buildFilings(response, recentFilings, selectedTicker.get());
    }

    private SubmissionsRecentFilings recentFilings(SubmissionsResponse response) {
        if (response.filings() == null) {
            return null;
        }
        return response.filings().recent();
    }

    /**
     * Pick a single representative ticker for the issuer, filtering OTC /
     * Pink listings when the parallel {@code exchanges} list provides
     * positive evidence. Tickers whose paired exchange is missing or empty
     * are kept (we don't drop on absence of evidence). Returns
     * {@link Optional#empty()} only when every ticker is positively
     * identified as OTC, or the tickers list is empty.
     *
     * <p>Among multiple non-OTC tickers, the SEC's listing order is
     * preserved (the SEC typically lists the primary class first). A
     * debug-level log records the disambiguation choice so the collapse
     * is traceable.
     */
    Optional<String> selectTicker(SubmissionsResponse response) {
        List<String> tickers = response.tickers();
        List<String> exchanges = response.exchanges();

        if (tickers == null || tickers.isEmpty()) {
            return Optional.empty();
        }

        List<String> usable = new ArrayList<>(tickers.size());
        for (int i = 0; i < tickers.size(); i++) {
            String ticker = tickers.get(i);
            if (ticker == null || ticker.isBlank()) {
                continue;
            }
            String exchange = i < exchanges.size() ? exchanges.get(i) : null;
            if (isOverTheCounter(exchange)) {
                continue;
            }
            usable.add(ticker.trim());
        }

        if (usable.isEmpty()) {
            return Optional.empty();
        }

        String selected = usable.get(0);
        if (usable.size() > 1) {
            log.debug("Multiple non-OTC tickers for cik={}: {} — selected {} (primary-class convention)",
                    response.cik(), usable, selected);
        }
        return Optional.of(selected);
    }

    /**
     * Returns {@code true} if the exchange name positively identifies a
     * ticker as an OTC / Pink listing. Case-insensitive; missing or empty
     * exchange names are NOT treated as OTC (we keep tickers we can't
     * positively classify).
     */
    private boolean isOverTheCounter(String exchange) {
        if (exchange == null || exchange.isBlank()) {
            return false;
        }
        String lower = exchange.toLowerCase();
        return lower.contains("otc") || lower.contains("pink");
    }

    private List<SecFiling> buildFilings(
            SubmissionsResponse response,
            SubmissionsRecentFilings recentFilings,
            String ticker
    ) {
        int filingCount = recentFilings.accessionNumber().size();
        List<SecFiling> filings = new ArrayList<>(filingCount);

        for (int index = 0; index < filingCount; index++) {
            String formType = recentFilings.form().get(index);
            if (isInScope(formType)) {
                filings.add(buildFiling(response, recentFilings, ticker, index));
            }
        }

        return filings;
    }

    /**
     * Returns true if {@code formType} is one of the canonical forms we
     * keep, OR is a documented variant of one (amendment suffix
     * {@code /A}, or the {@code 424B1..424B5} numbered prospectus series).
     *
     * <p>The SEC submissions payload mixes canonical forms with their
     * amendment and numbered variants in the same column. {@code Set.contains}
     * alone would silently drop {@code 8-K/A}, {@code 4/A}, {@code SC 13D/A},
     * and every concrete {@code 424B*} — see lib-ingestion REVIEW.md
     * Finding #24. We accept variants surgically (only the documented
     * ones) so unrelated forms that happen to share a prefix — e.g.
     * {@code 40-F} (foreign issuer annual report) or {@code S-11}
     * (real-estate registration) — stay correctly out of scope.
     */
    boolean isInScope(String formType) {
        if (formType == null) {
            return false;
        }
        String form = formType.trim();
        if (form.isEmpty()) {
            return false;
        }
        // Fast path — exact canonical forms.
        if (FORMS_IN_SCOPE.contains(form)) {
            return true;
        }
        // /A amendment of any canonical form.
        if (form.endsWith("/A")) {
            String stem = form.substring(0, form.length() - 2);
            if (FORMS_IN_SCOPE.contains(stem)) {
                return true;
            }
            // 424B1/A, 424B2/A, ...
            if (FOUR_TWO_FOUR_B_SUBFORMS.contains(stem)) {
                return true;
            }
        }
        // Numbered 424B prospectus variant (424B1..424B5).
        if (FOUR_TWO_FOUR_B_SUBFORMS.contains(form)) {
            return true;
        }
        return false;
    }

    private SecFiling buildFiling(
            SubmissionsResponse response,
            SubmissionsRecentFilings recentFilings,
            String ticker,
            int index
    ) {
        return new SecFiling(
                response.cik(),
                response.name(),
                ticker,
                recentFilings.accessionNumber().get(index),
                recentFilings.form().get(index).trim(),
                filingDate(recentFilings.filingDate().get(index)),
                acceptanceDateTime(recentFilings.acceptanceDateTime(), index),
                safeValue(recentFilings.primaryDocument(), index),
                safeValue(recentFilings.primaryDocDescription(), index),
                itemCodes(safeValue(recentFilings.items(), index))
        );
    }

    private LocalDate filingDate(String filingDate) {
        if (filingDate == null || filingDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(filingDate);
    }

    /**
     * Parse the SEC's {@code acceptanceDateTime} string at {@code index}
     * into an {@link Instant}. The SEC emits these timestamps in
     * Eastern time without an explicit zone ({@code 2026-05-13T16:01:23}
     * for a 4:01 PM ET filing); we treat the timestamp as
     * {@code America/New_York} local and convert to UTC.
     *
     * <p>Returns {@code null} when the column is missing entirely (older
     * fixtures) or empty at this row — the candidate builder falls back
     * to the {@code filingDate} anchor in that case.
     */
    private Instant acceptanceDateTime(List<String> column, int index) {
        if (column == null || index >= column.size()) {
            return null;
        }
        String raw = column.get(index);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            // The SEC submissions payload uses "YYYY-MM-DDTHH:MM:SS" in
            // Eastern time. java.time.LocalDateTime#parse accepts that
            // format natively; the zone is added explicitly.
            return java.time.LocalDateTime.parse(raw.trim())
                    .atZone(java.time.ZoneId.of("America/New_York"))
                    .toInstant();
        } catch (java.time.format.DateTimeParseException badFormat) {
            log.warn("Could not parse acceptanceDateTime=\"{}\" at index={} — falling back to filingDate",
                    raw, index);
            return null;
        }
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
