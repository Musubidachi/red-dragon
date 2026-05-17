package dev.reddragon.ingestion.services;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke tests for the manual ingestion path documented in lib-ingestion's
 * README ("manual entry").
 */
class ManualCandidateIngestionServiceTest {

    private final ManualCandidateIngestionService service = new ManualCandidateIngestionService();

    @Test
    void buildsCandidateWithManualSource() {
        TradeCandidate c = service.process(
                "NVDA", "NVIDIA",
                CandidateCatalystType.MANUAL_THESIS,
                "Headline", "Summary",
                0.8, 0.7, 0.65, 0.5
        );

        assertEquals("NVDA", c.symbol());
        assertEquals(SourceType.MANUAL, c.sourceType());
        assertEquals(CandidateCatalystType.MANUAL_THESIS, c.catalystType());
        assertNotNull(c.candidateId());
        assertFalse(c.candidateId().isBlank());
    }

    @Test
    void nullCatalystTypeDefaultsToManualThesis() {
        TradeCandidate c = service.process(
                "NVDA", "NVIDIA", null,
                "Headline", "Summary",
                0.8, 0.7, 0.65, 0.5
        );
        assertEquals(CandidateCatalystType.MANUAL_THESIS, c.catalystType());
    }
}
