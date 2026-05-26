package dev.reddragon.ingestion.services.sec;

import dev.reddragon.ingestion.models.sec.SecFiling;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the heuristics that replaced the {@code return 0.50}
 * placeholders for material significance and reflexivity.
 */
class SecFilingScoringHeuristicsTest {

    private final SecFilingScoringHeuristics heuristics = new SecFilingScoringHeuristics();

    @Test
    void nullFilingReturnsConservativeMaterialityAndReflexivity() {
        assertEquals(0.35, heuristics.materiality(null));
        assertEquals(0.30, heuristics.reflexivity(null));
    }

    @Test
    void eightKBankruptcyItemScoresHighMateriality() {
        SecFiling bankruptcy = eightK("1.03"); // Bankruptcy or Receivership
        assertEquals(0.90, heuristics.materiality(bankruptcy));
    }

    @Test
    void eightKEarningsScoresMediumMaterialityHighReflexivity() {
        SecFiling earnings = eightK("2.02"); // Results of Operations
        assertEquals(0.55, heuristics.materiality(earnings));
        assertEquals(0.75, heuristics.reflexivity(earnings), 1e-9);
    }

    @Test
    void scoresAreClampedToZeroOneRange() {
        SecFiling earnings = eightK("2.02");
        double m = heuristics.materiality(earnings);
        double r = heuristics.reflexivity(earnings);
        assertTrue(m >= 0.0 && m <= 1.0);
        assertTrue(r >= 0.0 && r <= 1.0);
    }

    @Test
    void schwabSC13DScoresHighMateriality() {
        SecFiling sc13d = otherForm("SC 13D");
        assertEquals(0.75, heuristics.materiality(sc13d));
    }

    private SecFiling eightK(String itemCode) {
        return new SecFiling(
                "1", "ACME", "ACME",
                "acc-1", "8-K", LocalDate.parse("2026-05-13"),
                "doc.htm", "desc",
                List.of(itemCode)
        );
    }

    private SecFiling otherForm(String formType) {
        return new SecFiling(
                "1", "ACME", "ACME",
                "acc-1", formType, LocalDate.parse("2026-05-13"),
                "doc.htm", "desc",
                List.of()
        );
    }
}
