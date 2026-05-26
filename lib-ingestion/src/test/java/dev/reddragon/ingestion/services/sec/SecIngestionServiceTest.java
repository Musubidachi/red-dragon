package dev.reddragon.ingestion.services.sec;

import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsRecentFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for SecIngestionService - the top-level lib-ingestion entry
 * documented in the README.
 *
 * <p>The service is composition only: it asks SubmissionsClient for the SEC
 * payload, asks SubmissionsFilingExtractor to flatten, then asks
 * SecCandidateBuilder to build one candidate per filing. We mock the HTTP
 * client and exercise the real extractor + builder downstream.
 */
class SecIngestionServiceTest {

    @Test
    void nullCikRejected() {
        SecIngestionService service = new SecIngestionService(
                mock(SubmissionsClient.class),
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics())
        );

        assertThrows(NullPointerException.class, () -> service.process(null));
    }

    @Test
    void processEmptyFilingsReturnsEmptyList() {
        SubmissionsClient client = mock(SubmissionsClient.class);
        when(client.process("12345")).thenReturn(new SubmissionsResponse(
                "12345", "ACME", List.of("ACME"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
                ))
        ));

        SecIngestionService service = new SecIngestionService(
                client,
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics())
        );

        List<TradeCandidate> candidates = service.process("12345");

        assertEquals(0, candidates.size());
        verify(client).process("12345");
    }

    @Test
    void process8KFilingProducesOneCandidatePerInScopeFiling() {
        SubmissionsClient client = mock(SubmissionsClient.class);
        // Two filings: one 8-K (in scope), one DEF 14A (out of scope and should be filtered)
        when(client.process(eq("0000320193"))).thenReturn(new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("0001-25-001", "0001-25-002"),
                        List.of("2026-05-01",  "2026-04-15"),
                        List.of("8-K",         "DEF 14A"),
                        List.of("doc1.htm",    "proxy.htm"),
                        List.of("8-K",         "Proxy"),
                        List.of("2.02",        "")
                ))
        ));

        SecIngestionService service = new SecIngestionService(
                client,
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics())
        );

        List<TradeCandidate> candidates = service.process("0000320193");

        assertEquals(1, candidates.size(), "only the 8-K should make it through");
        TradeCandidate only = candidates.get(0);
        assertEquals("AAPL", only.symbol());
        assertEquals("0001-25-001", only.candidateId(), "accession number is the candidate id (idempotency)");
    }

    @Test
    void processByTickerResolvesCikBeforeFetchingSubmissions() {
        CikLookupService lookupService = mock(CikLookupService.class);
        when(lookupService.process("AAPL")).thenReturn(Optional.of("0000320193"));

        SubmissionsClient client = mock(SubmissionsClient.class);
        when(client.process(eq("0000320193"))).thenReturn(new SubmissionsResponse(
                "0000320193", "Apple Inc", List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of("0001-25-001"),
                        List.of("2026-05-01"),
                        List.of("8-K"),
                        List.of("doc1.htm"),
                        List.of("8-K"),
                        List.of("2.02")
                ))
        ));

        SecIngestionService service = new SecIngestionService(
                client,
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics()),
                lookupService
        );

        List<TradeCandidate> candidates = service.processByTicker("AAPL");

        assertEquals(1, candidates.size());
        assertEquals("AAPL", candidates.get(0).symbol());
        verify(lookupService).process("AAPL");
        verify(client).process("0000320193");
    }

    @Test
    void processByTickerRejectsUnknownTicker() {
        CikLookupService lookupService = mock(CikLookupService.class);
        when(lookupService.process("MISSING")).thenReturn(Optional.empty());
        SecIngestionService service = new SecIngestionService(
                mock(SubmissionsClient.class),
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics()),
                lookupService
        );

        assertThrows(IllegalArgumentException.class, () -> service.processByTicker("MISSING"));
    }
}
