package dev.reddragon.persistence.services;

import dev.reddragon.persistence.PersistenceIntegrationTestApplication;
import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.domains.BacktestResultEntity;
import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.MarketQuoteObservationEntity;
import dev.reddragon.persistence.domains.MarketSnapshotEntity;
import dev.reddragon.persistence.domains.SchwabTokenEntity;
import dev.reddragon.persistence.domains.TradeHistoryImportBatchEntity;
import dev.reddragon.persistence.domains.TraderNoteEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;
import dev.reddragon.persistence.domains.VerdictOverrideEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.BacktestResultRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.MarketQuoteObservationRepository;
import dev.reddragon.persistence.services.repositories.MarketSnapshotRepository;
import dev.reddragon.persistence.services.repositories.SchwabTokenRepository;
import dev.reddragon.persistence.services.repositories.TradeHistoryImportBatchRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictReasonRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = PersistenceIntegrationTestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:persistence_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Transactional
class PersistenceRepositoryIntegrationTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-05-20T12:00:00Z");
    private static final String SCHWAB_TOKEN_KEY_PROPERTY = "red-dragon.persistence.schwab-token-encryption-key";
    private static final String SCHWAB_CIPHERTEXT_PREFIX = "rdg:v1:";
    private static final String SCHWAB_KEY = "base64:" + Base64.getEncoder().encodeToString(keyBytes(7));

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Autowired
    private MarketSnapshotRepository marketSnapshotRepository;

    @Autowired
    private MarketQuoteObservationRepository marketQuoteObservationRepository;

    @Autowired
    private TraderNoteRepository traderNoteRepository;

    @Autowired
    private TradeHistoryImportBatchRepository tradeHistoryImportBatchRepository;

    @Autowired
    private ValidationVerdictRepository validationVerdictRepository;

    @Autowired
    private ValidationVerdictReasonRepository validationVerdictReasonRepository;

    @Autowired
    private VerdictOverrideRepository verdictOverrideRepository;

    @Autowired
    private BacktestResultRepository backtestResultRepository;

    @Autowired
    private SchwabTokenRepository schwabTokenRepository;

    @AfterEach
    void clearSchwabKey() {
        System.clearProperty(SCHWAB_TOKEN_KEY_PROPERTY);
    }

    @Test
    void flywayMigratesThroughFreeformTextVersionAndJpaValidatesSchema() {
        assertNotNull(flyway.info().current());
        assertEquals("16", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void repositoriesRoundTripLongFreeformTextAfterFlywayMigration() {
        String longText = longText("freeform-text", 9_000);
        CandidateEntity candidate = candidateRepository.saveAndFlush(candidate("long-text-candidate", longText));

        AnalyticsSnapshotEntity analytics = analyticsSnapshotRepository.saveAndFlush(AnalyticsSnapshotEntity.builder()
                .candidateId(candidate.getCandidateId())
                .symbol(candidate.getSymbol())
                .observedAt(OBSERVED_AT)
                .regimeLabel("EVENT_DRIVEN")
                .regimeCompatibilityScore(0.72)
                .asymmetryScore(0.63)
                .equilibriumQualityScore(0.58)
                .reflexivityPotentialScore(0.61)
                .deploymentConfidenceScore(0.67)
                .reasonNotes(longText)
                .build());
        MarketSnapshotEntity marketSnapshot = marketSnapshotRepository.saveAndFlush(MarketSnapshotEntity.builder()
                .candidateId(candidate.getCandidateId())
                .symbol(candidate.getSymbol())
                .observedAt(OBSERVED_AT)
                .latestClose(101.0)
                .previousClose(99.5)
                .gapPercent(1.5)
                .averageTrueRange(2.1)
                .rangePosition(0.7)
                .averageVolume(1_500_000.0)
                .liquidityScore(0.8)
                .volatilityStabilityScore(0.74)
                .quality("VALID")
                .notes(longText)
                .relativeVolume(1.4)
                .vwapDeviation(0.02)
                .directionalPersistence(0.66)
                .build());
        MarketQuoteObservationEntity quote = marketQuoteObservationRepository.saveAndFlush(
                MarketQuoteObservationEntity.builder()
                        .symbol(candidate.getSymbol())
                        .observedAt(OBSERVED_AT)
                        .lastPrice(101.25)
                        .bidPrice(101.10)
                        .askPrice(101.40)
                        .volume(2_000_000L)
                        .quality("VALID")
                        .notes(longText)
                        .build());
        TraderNoteEntity note = traderNoteRepository.saveAndFlush(TraderNoteEntity.builder()
                .candidateId(candidate.getCandidateId())
                .symbol(candidate.getSymbol())
                .noteText(longText)
                .createdAt(OBSERVED_AT)
                .author("integration-test")
                .build());
        TradeHistoryImportBatchEntity importBatch = tradeHistoryImportBatchRepository.saveAndFlush(
                TradeHistoryImportBatchEntity.builder()
                        .importedAt(OBSERVED_AT)
                        .totalRows(3)
                        .importedRows(2)
                        .warnings(longText)
                        .build());

        ValidationVerdictEntity verdict = validationVerdictRepository.saveAndFlush(verdict(candidate, longText));
        VerdictOverrideEntity override = verdictOverrideRepository.saveAndFlush(VerdictOverrideEntity.builder()
                .verdictId(verdict.getId())
                .candidateId(candidate.getCandidateId())
                .symbol(candidate.getSymbol())
                .originalVerdict("WATCH")
                .overrideVerdict("PASS")
                .reason(longText)
                .overriddenAt(OBSERVED_AT)
                .author("integration-test")
                .build());

        entityManager.flush();
        entityManager.clear();

        assertEquals(longText, candidateRepository.findById(candidate.getCandidateId()).orElseThrow().getSummary());
        assertEquals(longText, analyticsSnapshotRepository.findById(analytics.getId()).orElseThrow().getReasonNotes());
        assertEquals(longText, marketSnapshotRepository.findById(marketSnapshot.getId()).orElseThrow().getNotes());
        assertEquals(longText, marketQuoteObservationRepository.findById(quote.getId()).orElseThrow().getNotes());
        assertEquals(longText, traderNoteRepository.findById(note.getId()).orElseThrow().getNoteText());
        assertEquals(longText, tradeHistoryImportBatchRepository.findById(importBatch.getId()).orElseThrow().getWarnings());
        assertEquals(longText, verdictOverrideRepository.findById(override.getId()).orElseThrow().getReason());

        List<ValidationVerdictReasonEntity> reasons =
                validationVerdictReasonRepository.findByVerdict_IdOrderBySortOrderAsc(verdict.getId());
        assertEquals(1, reasons.size());
        assertEquals(longText, reasons.get(0).getExplanation());
        assertEquals(1, validationVerdictReasonRepository.countByReasonCode("LONG_TEXT_REASON"));
    }

    @Test
    void backtestRunCandidateUniquenessIsEnforcedByMigration() {
        CandidateEntity candidate = candidateRepository.saveAndFlush(candidate("backtest-candidate", "short summary"));
        backtestResultRepository.saveAndFlush(backtest(candidate, "run-2026-05-20"));

        BacktestResultEntity duplicate = backtest(candidate, "run-2026-05-20");

        assertThrows(DataIntegrityViolationException.class,
                () -> backtestResultRepository.saveAndFlush(duplicate));
    }

    @Test
    void schwabTokenRepositoryEncryptsStoredTokensAndReturnsPlaintext() {
        System.setProperty(SCHWAB_TOKEN_KEY_PROPERTY, SCHWAB_KEY);
        SchwabTokenEntity saved = schwabTokenRepository.saveAndFlush(SchwabTokenEntity.builder()
                .accessToken("access-token-value")
                .refreshToken("refresh-token-value")
                .issuedAt(OBSERVED_AT)
                .expiresAt(OBSERVED_AT.plusSeconds(3600))
                .tokenType("Bearer")
                .build());

        entityManager.flush();

        String storedAccessToken = jdbcTemplate.queryForObject(
                "select access_token from schwab_token where id = ?",
                String.class,
                saved.getId());
        String storedRefreshToken = jdbcTemplate.queryForObject(
                "select refresh_token from schwab_token where id = ?",
                String.class,
                saved.getId());
        assertNotNull(storedAccessToken);
        assertNotNull(storedRefreshToken);
        assertTrue(storedAccessToken.startsWith(SCHWAB_CIPHERTEXT_PREFIX));
        assertTrue(storedRefreshToken.startsWith(SCHWAB_CIPHERTEXT_PREFIX));
        assertFalse(storedAccessToken.contains("access-token-value"));
        assertFalse(storedRefreshToken.contains("refresh-token-value"));

        entityManager.clear();

        SchwabTokenEntity latest = schwabTokenRepository.findTopByOrderByIssuedAtDesc();
        assertEquals("access-token-value", latest.getAccessToken());
        assertEquals("refresh-token-value", latest.getRefreshToken());
    }

    private static CandidateEntity candidate(String candidateId, String summary) {
        return CandidateEntity.builder()
                .candidateId(candidateId)
                .symbol("ACME")
                .companyName("Acme Inc")
                .catalystType("GOVERNMENT_GRANT")
                .sourceType("SEC_EDGAR")
                .sourceId(candidateId)
                .sourceUrl("https://example.com/" + candidateId)
                .observedAt(OBSERVED_AT)
                .headline("Acme wins contract")
                .summary(summary)
                .build();
    }

    private static ValidationVerdictEntity verdict(CandidateEntity candidate, String explanation) {
        ValidationVerdictEntity verdict = ValidationVerdictEntity.builder()
                .candidateId(candidate.getCandidateId())
                .symbol(candidate.getSymbol())
                .verdict("WATCH")
                .deploymentTier("STANDARD")
                .score(0.67)
                .idempotencyKey("vv:" + candidate.getCandidateId())
                .reasonCodes(null)
                .explanations(null)
                .createdAt(OBSERVED_AT)
                .reasons(new ArrayList<>())
                .build();
        verdict.getReasons().add(ValidationVerdictReasonEntity.builder()
                .verdict(verdict)
                .reasonCode("LONG_TEXT_REASON")
                .explanation(explanation)
                .sortOrder((short) 0)
                .build());
        return verdict;
    }

    private static BacktestResultEntity backtest(CandidateEntity candidate, String runId) {
        return BacktestResultEntity.builder()
                .runId(runId)
                .strategyName("integration-strategy")
                .symbol(candidate.getSymbol())
                .candidateId(candidate.getCandidateId())
                .verdict("WATCH")
                .score(0.67)
                .testedAt(OBSERVED_AT)
                .build();
    }

    private static String longText(String prefix, int length) {
        return prefix + "-" + "x".repeat(length);
    }

    private static byte[] keyBytes(int start) {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (start + i);
        }
        return key;
    }
}
