package dev.reddragon.ingestion.services.sec;

import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsRecentFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the column-to-row extractor that flattens SEC's payload.
 */
class SubmissionsFilingExtractorTest {

    private final SubmissionsFilingExtractor extractor = new SubmissionsFilingExtractor();

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
}
