package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.domain.models.TradeCandidate;
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

    @Test
    void acceptanceDateTimePreferredOverFilingDateForEarlyness() {
        // The bug this guards (lib-ingestion REVIEW.md Finding #18):
        // a filing made at 4 PM ET on 2026-05-12 (= 2026-05-12T20:00Z)
        // anchored at filingDate=2026-05-12 produces observedAt =
        // 2026-05-12T00:00Z — 20 hours BEFORE the actual filing. Earlyness
        // tier (now 3 hours after the real filing) should still be the
        // freshest bucket. Without acceptanceDateTime, the same fixture
        // would report 23 hours elapsed and drop one tier.
        Clock clock = Clock.fixed(Instant.parse("2026-05-12T23:00:00Z"), ZoneOffset.UTC);
        SecCandidateBuilder builder = new SecCandidateBuilder(
                new EightKCategoryMapper(),
                new SecFilingScoringHeuristics(),
                clock);

        Instant acceptedAt = Instant.parse("2026-05-12T20:00:00Z"); // 4 PM ET
        SecFiling withAcceptance = new SecFiling(
                "00001234", "ACME", "ACME", "a1", "8-K",
                LocalDate.of(2026, 5, 12),
                acceptedAt,
                "doc.htm", "desc", List.of());

        TradeCandidate candidate = builder.process(withAcceptance);

        // 3 hours since accepted; well under the 4-hour fresh tier (0.90).
        assertEquals(0.90, candidate.earlynessScore());
    }

    @Test
    void filingDateFallbackUsedWhenAcceptanceMissing() {
        // Same filingDate, but no acceptanceDateTime — anchor reverts
        // to filingDate.atStartOfDay(UTC), which is 23 hours back. That
        // drops the tier from 0.90 to 0.75. This locks in the documented
        // fallback behaviour so future refactors don't lose it.
        Clock clock = Clock.fixed(Instant.parse("2026-05-12T23:00:00Z"), ZoneOffset.UTC);
        SecCandidateBuilder builder = new SecCandidateBuilder(
                new EightKCategoryMapper(),
                new SecFilingScoringHeuristics(),
                clock);

        SecFiling withoutAcceptance = new SecFiling(
                "00001234", "ACME", "ACME", "a1", "8-K",
                LocalDate.of(2026, 5, 12),
                /* acceptanceDateTime */ null,
                "doc.htm", "desc", List.of());

        TradeCandidate candidate = builder.process(withoutAcceptance);
        assertEquals(0.75, candidate.earlynessScore());
    }
}
