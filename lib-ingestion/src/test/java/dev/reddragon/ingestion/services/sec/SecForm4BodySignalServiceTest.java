package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.SecFiling;

class SecForm4BodySignalServiceTest {

    @Test
    void fetchesParsesAndBuildsForm4BodyCandidate() throws IOException {
        RecordingSecHttpClient httpClient = new RecordingSecHttpClient(fixture("form4-ownership.xml"));
        URI primaryDocumentUri = URI.create(
                "https://www.sec.gov/Archives/edgar/data/320193/000032019326000001/form4.xml");

        Clock clock = Clock.fixed(Instant.parse("2026-05-21T21:00:00Z"), ZoneOffset.UTC);
        SecForm4BodySignalService service = new SecForm4BodySignalService(
                new SecFilingBodyClient(httpClient),
                new Form4OwnershipXmlParser(),
                clock);

        Optional<TradeCandidate> maybeCandidate = service.process(form4Filing(
                "0000320193-26-000001",
                "form4.xml",
                Instant.parse("2026-05-21T20:00:00Z")));

        assertTrue(maybeCandidate.isPresent());
        TradeCandidate candidate = maybeCandidate.get();
        assertEquals("sec-form4-body:0000320193-26-000001", candidate.candidateId());
        assertEquals("AAPL", candidate.symbol());
        assertEquals("APPLE INC", candidate.companyName());
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, candidate.catalystType());
        assertEquals(SourceType.SEC_EDGAR, candidate.sourceType());
        assertEquals("0000320193-26-000001:form4.xml", candidate.sourceId());
        assertEquals(primaryDocumentUri.toString(), candidate.sourceUrl());
        assertEquals(Instant.parse("2026-05-21T20:00:00Z"), candidate.observedAt());
        assertEquals(0.90, candidate.structuralRealityScore());
        assertEquals(0.55, candidate.materialSignificanceScore());
        assertEquals(0.90, candidate.earlynessScore());
        assertEquals(0.62, candidate.reflexivityPotentialScore());
        assertEquals("SEC Form 4 ownership activity - APPLE INC (AAPL)", candidate.headline());
        assertTrue(candidate.summary().contains("Cook Timothy D (officer: Chief Executive Officer)"));
        assertTrue(candidate.summary().contains("S 1000 shares at $195.25 on 2026-05-21"));
        assertTrue(candidate.summary().contains("S 500 shares at $196.1 on 2026-05-21"));
        assertEquals(primaryDocumentUri, httpClient.requestedUri());
    }

    @Test
    void returnsEmptyForNonForm4WithoutFetchingBody() {
        CountingSecFilingBodyClient bodyClient = new CountingSecFilingBodyClient("<ownershipDocument/>");
        SecForm4BodySignalService service = new SecForm4BodySignalService(
                bodyClient,
                new Form4OwnershipXmlParser());

        Optional<TradeCandidate> candidate = service.process(new SecFiling(
                "0000320193",
                "Apple Inc.",
                "AAPL",
                "0000320193-26-000002",
                "8-K",
                LocalDate.of(2026, 5, 21),
                "8k.htm",
                "8-K",
                List.of("2.02")
        ));

        assertTrue(candidate.isEmpty());
        assertEquals(0, bodyClient.calls());
    }

    @Test
    void fallsBackToOwnershipReportPeriodWhenAcceptanceTimestampIsMissing() throws IOException {
        RecordingSecHttpClient httpClient = new RecordingSecHttpClient(fixture("form4-gift-ownership.xml"));
        URI primaryDocumentUri = URI.create(
                "https://www.sec.gov/Archives/edgar/data/789019/000078901926000004/gift.xml");

        Clock clock = Clock.fixed(Instant.parse("2026-05-22T03:00:00Z"), ZoneOffset.UTC);
        SecForm4BodySignalService service = new SecForm4BodySignalService(
                new SecFilingBodyClient(httpClient),
                new Form4OwnershipXmlParser(),
                clock);

        Optional<TradeCandidate> maybeCandidate = service.process(new SecFiling(
                "0000789019",
                "Microsoft Corp",
                "MSFT",
                "0000789019-26-000004",
                "4",
                LocalDate.of(2026, 5, 20),
                null,
                "gift.xml",
                "Form 4",
                List.of()
        ));

        assertTrue(maybeCandidate.isPresent());
        TradeCandidate candidate = maybeCandidate.get();
        assertEquals(Instant.parse("2026-05-22T00:00:00Z"), candidate.observedAt());
        assertEquals("MSFT", candidate.symbol());
        assertEquals(0.45, candidate.materialSignificanceScore());
        assertEquals(0.55, candidate.reflexivityPotentialScore());
        assertEquals(0.90, candidate.earlynessScore());
        assertEquals(primaryDocumentUri, httpClient.requestedUri());
    }

    private SecFiling form4Filing(String accessionNumber, String primaryDocument, Instant acceptedAt) {
        return new SecFiling(
                "0000320193",
                "Apple Inc.",
                "AAPL",
                accessionNumber,
                "4",
                LocalDate.of(2026, 5, 21),
                acceptedAt,
                primaryDocument,
                "Form 4",
                List.of()
        );
    }

    private String fixture(String name) throws IOException {
        String path = "/sec/" + name;
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, "Missing test fixture " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
