package dev.reddragon.persistence.services;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.ReasonCode;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.domain.models.Verdict;
import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.MarketBarEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for PersistenceMapper - the only lib-persistence service.
 *
 * <p>The mapper translates between lib-domain value objects and JPA entities.
 * Tests confirm each public method handles null inputs, copies fields one-for-one,
 * and converts enum-typed fields to the database string form.
 */
class PersistenceMapperTest {

    private final PersistenceMapper mapper = new PersistenceMapper();

    @Test
    void nullCandidateRejected() {
        assertThrows(NullPointerException.class, () -> mapper.toCandidateEntity(null));
    }

    @Test
    void toCandidateEntityCopiesAllFields() {
        TradeCandidate candidate = TradeCandidate.builder()
                .candidateId("acc-1")
                .symbol("ACME")
                .companyName("Acme Inc")
                .catalystType(CandidateCatalystType.GOVERNMENT_GRANT)
                .sourceType(SourceType.SEC_EDGAR)
                .sourceId("acc-1")
                .sourceUrl("https://example.com")
                .observedAt(Instant.parse("2026-05-13T00:00:00Z"))
                .headline("Headline")
                .summary("Summary")
                .structuralRealityScore(0.8)
                .materialSignificanceScore(0.7)
                .earlynessScore(0.7)
                .reflexivityPotentialScore(0.5)
                .build();

        CandidateEntity entity = mapper.toCandidateEntity(candidate);

        assertEquals("acc-1", entity.getCandidateId());
        assertEquals("ACME", entity.getSymbol());
        assertEquals("Acme Inc", entity.getCompanyName());
        assertEquals("GOVERNMENT_GRANT", entity.getCatalystType(),
                "enum is stored as its name() string");
        assertEquals("SEC_EDGAR", entity.getSourceType());
    }

    @Test
    void nullValidationResultRejected() {
        assertThrows(NullPointerException.class, () -> mapper.toValidationVerdictEntity(null));
    }

    @Test
    void toValidationVerdictEntityMapsReasonsIntoChildRows() {
        ValidationResult result = new ValidationResult(
                "c-1", "ACME",
                Verdict.PASS, DeploymentTier.STANDARD,
                0.78,
                List.of(),
                List.of(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED, ReasonCode.MATERIAL_IMPACT_HIGH),
                List.of("structural reality high", "material impact high")
        );

        ValidationVerdictEntity entity = mapper.toValidationVerdictEntity(result);

        assertEquals("c-1", entity.getCandidateId());
        assertEquals("ACME", entity.getSymbol());
        assertEquals("PASS", entity.getVerdict());
        assertEquals("STANDARD", entity.getDeploymentTier());
        assertEquals(0.78, entity.getScore(), 1e-9);
        assertTrue(entity.getIdempotencyKey().startsWith("vv:"));
        assertEquals(67, entity.getIdempotencyKey().length());
        // V12+ contract: legacy string blob columns are no longer written.
        assertNull(entity.getReasonCodes(), "legacy reason_codes column should be null post-V12");
        assertNull(entity.getExplanations(), "legacy explanations column should be null post-V12");
        // Reasons live in the normalized child collection now.
        assertEquals(2, entity.getReasons().size());

        ValidationVerdictReasonEntity first = entity.getReasons().get(0);
        assertEquals("STRUCTURAL_CATALYST_CONFIRMED", first.getReasonCode());
        assertEquals("structural reality high", first.getExplanation());
        assertEquals((short) 0, first.getSortOrder());
        assertSame(entity, first.getVerdict(),
                "child should back-reference the parent so Hibernate cascades the FK");

        ValidationVerdictReasonEntity second = entity.getReasons().get(1);
        assertEquals("MATERIAL_IMPACT_HIGH", second.getReasonCode());
        assertEquals("material impact high", second.getExplanation());
        assertEquals((short) 1, second.getSortOrder());

        // The convenience accessors should reconstruct the legacy joined strings.
        assertTrue(entity.legacyReasonCodes().contains("STRUCTURAL_CATALYST_CONFIRMED"));
        assertTrue(entity.legacyReasonCodes().contains("MATERIAL_IMPACT_HIGH"));
    }

    @Test
    void toValidationVerdictEntityBuildsStableSemanticIdempotencyKey() {
        ValidationResult result = new ValidationResult(
                "c-1", "ACME",
                Verdict.PASS, DeploymentTier.STANDARD,
                0.78,
                List.of(),
                List.of(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED),
                List.of("structural reality high")
        );
        ValidationResult changedResult = new ValidationResult(
                "c-1", "ACME",
                Verdict.PASS, DeploymentTier.STANDARD,
                0.78,
                List.of(),
                List.of(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED),
                List.of("structural reality changed")
        );

        String first = mapper.toValidationVerdictEntity(result).getIdempotencyKey();
        String second = mapper.toValidationVerdictEntity(result).getIdempotencyKey();
        String changed = mapper.toValidationVerdictEntity(changedResult).getIdempotencyKey();

        assertEquals(first, second);
        assertNotEquals(first, changed);
    }

    @Test
    void nullBarRejected() {
        assertThrows(NullPointerException.class, () -> mapper.toMarketBarEntity(null));
    }

    @Test
    void toMarketBarEntityCopiesPriceAndVolumeFields() {
        MarketBar bar = new MarketBar(
                "ACME", LocalDate.parse("2026-05-13"),
                100.0, 105.0, 99.0, 103.5, 1_500_000L
        );

        MarketBarEntity entity = mapper.toMarketBarEntity(bar);

        assertEquals("ACME", entity.getSymbol());
        assertEquals(LocalDate.parse("2026-05-13"), entity.getBarDate());
        assertEquals(100.0, entity.getOpenPrice());
        assertEquals(105.0, entity.getHighPrice());
        assertEquals(103.5, entity.getClosePrice());
        assertEquals(1_500_000L, entity.getVolume());
    }

    @Test
    void toMarketBarEntitiesReturnsEmptyForNullOrEmptyInput() {
        assertTrue(mapper.toMarketBarEntities(null).isEmpty());
        assertTrue(mapper.toMarketBarEntities(List.of()).isEmpty());
    }

    @Test
    void toMarketBarEntitiesMapsEachBar() {
        List<MarketBar> bars = List.of(
                new MarketBar("ACME", LocalDate.parse("2026-05-12"), 100, 101, 99, 100, 1_000_000L),
                new MarketBar("ACME", LocalDate.parse("2026-05-13"), 100, 101, 99, 100, 1_000_000L)
        );
        List<MarketBarEntity> entities = mapper.toMarketBarEntities(bars);
        assertEquals(2, entities.size());
    }
}
