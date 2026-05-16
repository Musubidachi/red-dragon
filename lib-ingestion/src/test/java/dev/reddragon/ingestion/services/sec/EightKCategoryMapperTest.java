package dev.reddragon.ingestion.services.sec;

import dev.reddragon.domain.models.CandidateCatalystType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link EightKCategoryMapper}.
 *
 * <p>The mapper is a pure function over a list of item codes, so every
 * test is a one-line assertion.
 */
class EightKCategoryMapperTest {

    private final EightKCategoryMapper mapper = new EightKCategoryMapper();

    @Test
    void empty_item_codes_fall_back_to_filing_event() {
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(List.of()));
    }

    @Test
    void null_item_codes_fall_back_to_filing_event() {
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(null));
    }

    @Test
    void bankruptcy_maps_to_structural_demand_change() {
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("1.03", "9.01")));
    }

    @Test
    void material_agreement_maps_to_contract() {
        assertEquals(CandidateCatalystType.CONTRACT, mapper.process(List.of("1.01", "9.01")));
    }

    @Test
    void mna_takes_priority_over_contract() {
        assertEquals(CandidateCatalystType.CONTRACT, mapper.process(List.of("2.01", "1.01")));
    }

    @Test
    void dilution_maps_to_structural_demand_change() {
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("3.02")));
    }

    @Test
    void earnings_only_is_classified_as_filing_event() {
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(List.of("2.02", "9.01")));
    }

    @Test
    void severe_negative_outranks_governance_when_both_present() {
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("5.02", "4.02")));
    }
}
