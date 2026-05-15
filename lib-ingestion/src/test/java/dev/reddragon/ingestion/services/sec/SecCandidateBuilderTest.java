package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.ingestion.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.SecFiling;

class SecCandidateBuilderTest {

    @Test
    void usesAccessionNumberAsCandidateIdForIdempotency() {
        SecCandidateBuilder builder = new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics());
        SecFiling filing = new SecFiling("00001234", "ACME", "ACME", "1234-5678", "8-K", LocalDate.now(), "doc.htm", "desc", List.of("2.03"));

        TradeCandidate candidate = builder.process(filing);

        assertEquals("1234-5678", candidate.candidateId());
    }

    @Test
    void earlynessScoreDropsAsFilingAges() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-13T03:00:00Z"), ZoneOffset.UTC);
        SecCandidateBuilder builder = new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics(), clock);
        SecFiling fresh = new SecFiling("00001234", "ACME", "ACME", "a1", "8-K", LocalDate.of(2026,5,13), "doc.htm", "desc", List.of());
        SecFiling old = new SecFiling("00001234", "ACME", "ACME", "a2", "8-K", LocalDate.of(2026,5,1), "doc.htm", "desc", List.of());

        TradeCandidate freshCandidate = builder.process(fresh);
        TradeCandidate oldCandidate = builder.process(old);

        assertEquals(0.90, freshCandidate.earlynessScore());
        assertEquals(0.30, oldCandidate.earlynessScore());
    }
}
