package dev.reddragon.ingestion.services.sec;

import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsRecentFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the column-to-row extractor that flattens SEC's payload.
 *
 * <p>The extractor also handles ticker selection (OTC filtering, multi-class
 * disambiguation, dropping tickerless filings) — those concerns are exercised
 * here too since they live in the same pass.
 */
class SubmissionsFilingExtractorTest {

    private final SubmissionsFilingExtractor extractor = new SubmissionsFilingExtractor();

    // ---- Basic processing -------------------------------------------------

    @Test
    void nullResponseRejected() {
        assertThrows(NullPointerException.class, () -> extractor.process(null));
    }

    @Test
    void inScopeFormsAreKeptAndOthersDropped() {
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("a1", "a2", "a3", "a4"),
                        List.of("2026-05-01", "2026-04-30", "2026-04-29", "2026-04-28"),
                        List.of("8-K", "DEF 14A", "4", "10-Q"),
                        List.of("d1.htm", "d2.htm", "d3.htm", "d4.htm"),
                        List.of("8-K body", "Proxy", "Form 4", "10-Q"),
                        List.of("2.02", "", "", "")
                ))
        );

        List<SecFiling> filings = extractor.process(response);

        assertEquals(2, filings.size(), "8-K and Form 4 should be kept");
        assertTrue(filings.stream().anyMatch(f -> "8-K".equals(f.formType())));
        assertTrue(filings.stream().anyMatch(f -> "4".equals(f.formType())));
    }

    @Test
    void itemCodesParsedFromCommaList() {
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("a1"),
                        List.of("2026-05-01"),
                        List.of("8-K"),
                        List.of("d1.htm"),
                        List.of("8-K body"),
                        List.of("1.01,2.02,9.01")
                ))
        );

        SecFiling filing = extractor.process(response).get(0);
        assertEquals(List.of("1.01", "2.02", "9.01"), filing.itemCodes());
    }

    @Test
    void nullRecentBlockReturnsEmpty() {
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(null)
        );
        assertTrue(extractor.process(response).isEmpty());
    }

    // ---- acceptanceDateTime parsing (REVIEW.md Finding #18/#19) ----------

    @Test
    void acceptanceDateTimeParsedAsEasternTime() {
        // SEC emits "2026-05-12T16:01:23" as Eastern time without zone.
        // 4:01 PM ET on 2026-05-12 = 2026-05-12T20:01:23Z (EDT = UTC-4).
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("a1"),
                        List.of("2026-05-12"),
                        List.of("8-K"),
                        List.of("d1.htm"),
                        List.of("8-K body"),
                        List.of("2.02"),
                        List.of("2026-05-12T16:01:23"),
                        List.of(1),
                        List.of(1)
                ))
        );

        SecFiling filing = extractor.process(response).get(0);
        assertEquals(java.time.Instant.parse("2026-05-12T20:01:23Z"),
                filing.acceptanceDateTime());
    }

    @Test
    void acceptanceDateTimeMissingFallsBackToNull() {
        // Older fixtures don't carry acceptanceDateTime — extractor must
        // pass null through so SecCandidateBuilder can fall back to
        // filingDate.atStartOfDay(UTC).
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                oneEightK()
        );

        SecFiling filing = extractor.process(response).get(0);
        org.junit.jupiter.api.Assertions.assertNull(filing.acceptanceDateTime());
    }

    @Test
    void malformedAcceptanceDateTimeDoesNotKillTheRow() {
        // A garbled timestamp must not blow up the whole submissions
        // response — the row is kept, acceptanceDateTime is null, and
        // the extractor logs a warning (asserted indirectly via the
        // null observation here).
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("a1"),
                        List.of("2026-05-12"),
                        List.of("8-K"),
                        List.of("d1.htm"),
                        List.of("8-K body"),
                        List.of("2.02"),
                        List.of("not-a-timestamp"),
                        List.of(),
                        List.of()
                ))
        );

        List<SecFiling> filings = extractor.process(response);
        assertEquals(1, filings.size());
        org.junit.jupiter.api.Assertions.assertNull(filings.get(0).acceptanceDateTime());
    }

    // ---- Form variant matching (REVIEW.md Finding #24) -------------------

    @Test
    void amendmentVariantsAreInScope() {
        // /A amendment suffixes are real SEC forms. They must survive the
        // in-scope filter the same as the canonical parent.
        assertTrue(extractor.isInScope("8-K/A"));
        assertTrue(extractor.isInScope("4/A"));
        assertTrue(extractor.isInScope("SC 13D/A"));
        assertTrue(extractor.isInScope("SC 13G/A"));
        assertTrue(extractor.isInScope("S-1/A"));
        assertTrue(extractor.isInScope("S-3/A"));
    }

    @Test
    void numbered424BSubformsAreInScope() {
        // The SEC submissions payload never carries "424B" alone — it lists
        // the concrete subform (424B1..424B5). All must be accepted.
        assertTrue(extractor.isInScope("424B1"));
        assertTrue(extractor.isInScope("424B2"));
        assertTrue(extractor.isInScope("424B3"));
        assertTrue(extractor.isInScope("424B4"));
        assertTrue(extractor.isInScope("424B5"));
        // Amendment of a numbered prospectus is also valid.
        assertTrue(extractor.isInScope("424B3/A"));
    }

    @Test
    void unrelatedFormsSharingAPrefixStayOutOfScope() {
        // "40-F" is the foreign-issuer annual report — startsWith("4") but
        // not actually a Form 4 variant. The prior overly-broad prefix
        // match would have let this through.
        assertFalse(extractor.isInScope("40-F"));
        // "S-11" (real estate registration) shares "S-1" as a prefix but
        // is a different form.
        assertFalse(extractor.isInScope("S-11"));
        // "424B99" — invented future subform; deliberately rejected until
        // explicitly added to FOUR_TWO_FOUR_B_SUBFORMS.
        assertFalse(extractor.isInScope("424B99"));
    }

    @Test
    void amendmentsAndNumberedSubformsFlowThroughTheFullPipeline() {
        // End-to-end: a payload mixing canonical 8-K, 8-K/A, 424B3, and an
        // out-of-scope DEF 14A keeps the three in-scope rows and drops one.
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("a1", "a2", "a3", "a4"),
                        List.of("2026-05-01", "2026-04-30", "2026-04-29", "2026-04-28"),
                        List.of("8-K", "8-K/A", "424B3", "DEF 14A"),
                        List.of("d1.htm", "d2.htm", "d3.htm", "d4.htm"),
                        List.of("8-K body", "8-K amendment", "Prospectus", "Proxy"),
                        List.of("2.02", "2.02", "", "")
                ))
        );

        List<SecFiling> filings = extractor.process(response);

        assertEquals(3, filings.size(), "8-K, 8-K/A, and 424B3 all kept; DEF 14A dropped");
        assertTrue(filings.stream().anyMatch(f -> "8-K/A".equals(f.formType())));
        assertTrue(filings.stream().anyMatch(f -> "424B3".equals(f.formType())));
    }

    // ---- Ticker selection -------------------------------------------------

    @Test
    void singleTickerWithoutExchangeIsKept() {
        // Backwards-compatible callers don't supply exchanges; we keep the
        // ticker because we have no positive evidence it's OTC.
        SubmissionsResponse response = new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                oneEightK()
        );

        List<SecFiling> filings = extractor.process(response);
        assertEquals(1, filings.size());
        assertEquals("AAPL", filings.get(0).ticker());
    }

    @Test
    void tickersWithoutExchangesArePairedWithBlankAndKept() {
        // exchanges shorter than tickers — defensive: keep the unmatched ones.
        SubmissionsResponse response = new SubmissionsResponse(
                "0000123456", "Issuer",
                List.of("ABC", "ABCD"),
                List.of("NYSE"),                  // only first ticker has exchange info
                oneEightK()
        );

        SecFiling filing = extractor.process(response).get(0);
        assertEquals("ABC", filing.ticker(), "first non-OTC ticker selected, SEC order preserved");
    }

    @Test
    void otcOnlyIssuerDropsAllFilings() {
        SubmissionsResponse response = new SubmissionsResponse(
                "0000777777", "Pink Co",
                List.of("PINKK"),
                List.of("OTC Pink"),
                oneEightK()
        );

        assertTrue(extractor.process(response).isEmpty(),
                "issuer whose only ticker is OTC should be dropped entirely");
    }

    @Test
    void otcTickerIsFilteredOutWhenAListedSiblingExists() {
        // Multi-class issuer: one ticker on a real exchange, one on OTC.
        SubmissionsResponse response = new SubmissionsResponse(
                "0000999999", "Dual Co",
                List.of("DUAL", "DUALP"),
                List.of("NASDAQ", "OTC Pink"),
                oneEightK()
        );

        SecFiling filing = extractor.process(response).get(0);
        assertEquals("DUAL", filing.ticker(), "OTC sibling filtered, real-exchange ticker kept");
    }

    @Test
    void multipleNonOtcTickersResolveToFirstByConvention() {
        // Multi-class issuer like GOOG (Class C) / GOOGL (Class A). The SEC
        // typically lists the primary class first, so we preserve order.
        SubmissionsResponse response = new SubmissionsResponse(
                "0001652044", "Alphabet Inc",
                List.of("GOOG", "GOOGL"),
                List.of("NASDAQ", "NASDAQ"),
                oneEightK()
        );

        SecFiling filing = extractor.process(response).get(0);
        assertEquals("GOOG", filing.ticker(),
                "primary class (first in SEC's list) wins when both are non-OTC");
    }

    @Test
    void emptyTickerListDropsAllFilings() {
        SubmissionsResponse response = new SubmissionsResponse(
                "0000000000", "Private Co",
                List.of(),
                oneEightK()
        );

        assertTrue(extractor.process(response).isEmpty(),
                "issuer with no tickers should not emit candidates");
    }

    @Test
    void blankTickersAreSkippedEvenWithExchangeData() {
        SubmissionsResponse response = new SubmissionsResponse(
                "0000444444", "Half-Listed Co",
                List.of("", "REAL"),
                List.of("NYSE", "NYSE"),
                oneEightK()
        );

        SecFiling filing = extractor.process(response).get(0);
        assertEquals("REAL", filing.ticker(),
                "blank ticker skipped, next non-OTC ticker selected");
    }

    // ---- Fixtures ---------------------------------------------------------

    /** Single 8-K filing — the simplest in-scope payload for ticker tests. */
    private static SubmissionsFilings oneEightK() {
        return new SubmissionsFilings(new SubmissionsRecentFilings(
                List.of("a1"),
                List.of("2026-05-01"),
                List.of("8-K"),
                List.of("d1.htm"),
                List.of("8-K body"),
                List.of("2.02")
        ));
    }
}
