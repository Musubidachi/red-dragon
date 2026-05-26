package dev.reddragon.ingestion.services.sec;

import dev.reddragon.domain.models.CandidateCatalystType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link EightKCategoryMapper}.
 *
 * <p>The mapper is a pure function over a list of item codes, so every
 * test is a one-line assertion. Tests are organized by bucket; the
 * priority-order tests assert which bucket wins when multiple are present.
 */
class EightKCategoryMapperTest {

    private final EightKCategoryMapper mapper = new EightKCategoryMapper();

    // ---- Edge / fallback cases --------------------------------------------

    @Test
    void empty_item_codes_fall_back_to_filing_event() {
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(List.of()));
    }

    @Test
    void null_item_codes_fall_back_to_filing_event() {
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(null));
    }

    @Test
    void exhibits_only_falls_back_to_filing_event() {
        // 9.01 alone has no signal — only declares attached exhibits.
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(List.of("9.01")));
    }

    // ---- SEVERE bucket (1.03, 2.04, 2.05, 2.06, 3.01, 4.02) ---------------

    @Test
    void bankruptcy_maps_to_structural_demand_change() {
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("1.03", "9.01")));
    }

    @Test
    void debt_acceleration_maps_to_structural_demand_change() {
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("2.04")));
    }

    @Test
    void exit_or_disposal_of_business_maps_to_structural_demand_change() {
        // Item 2.05 — winding down a business segment is a severe-negative event.
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("2.05")));
    }

    @Test
    void material_impairment_maps_to_structural_demand_change() {
        // Item 2.06 — material impairments wipe value like restatements do.
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("2.06")));
    }

    // ---- M&A bucket (2.01, 5.01) ------------------------------------------

    @Test
    void mna_acquisition_maps_to_merger_and_acquisition() {
        // Item 2.01 — completion of acquisition or disposition of assets.
        assertEquals(CandidateCatalystType.MERGER_AND_ACQUISITION, mapper.process(List.of("2.01")));
    }

    @Test
    void mna_change_of_control_maps_to_merger_and_acquisition() {
        // Item 5.01 — change of control.
        assertEquals(CandidateCatalystType.MERGER_AND_ACQUISITION, mapper.process(List.of("5.01")));
    }

    @Test
    void mna_takes_priority_over_contract_when_both_present() {
        // M&A subsumes the underlying material agreement.
        assertEquals(CandidateCatalystType.MERGER_AND_ACQUISITION, mapper.process(List.of("2.01", "1.01")));
    }

    // ---- CONTRACT bucket (1.01, 1.02) -------------------------------------

    @Test
    void material_agreement_maps_to_contract() {
        assertEquals(CandidateCatalystType.CONTRACT, mapper.process(List.of("1.01", "9.01")));
    }

    @Test
    void material_agreement_termination_maps_to_contract() {
        assertEquals(CandidateCatalystType.CONTRACT, mapper.process(List.of("1.02")));
    }

    // ---- DILUTION bucket (2.03, 3.02, 3.03) -------------------------------

    @Test
    void unregistered_sales_maps_to_structural_demand_change() {
        // 3.02 — unregistered sales of equity securities.
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("3.02")));
    }

    @Test
    void new_debt_obligation_maps_to_structural_demand_change() {
        // 2.03 — creation of direct financial obligation. Same capital-structure
        // family as 3.02/3.03.
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("2.03")));
    }

    // ---- DISCLOSURE bucket (7.01, 8.01) -----------------------------------

    @Test
    void reg_fd_disclosure_maps_to_news_event() {
        // 7.01 — Regulation FD Disclosure. Where FDA approvals, partnerships,
        // government-contract announcements typically attach.
        assertEquals(CandidateCatalystType.NEWS_EVENT, mapper.process(List.of("7.01", "9.01")));
    }

    @Test
    void other_events_disclosure_maps_to_news_event() {
        assertEquals(CandidateCatalystType.NEWS_EVENT, mapper.process(List.of("8.01")));
    }

    @Test
    void disclosure_paired_with_exhibits_still_routes_to_news_event() {
        // The common press-release shape: 7.01 + 9.01 (the exhibit attaching
        // the release). Previously fell through to FILING_EVENT.
        assertEquals(CandidateCatalystType.NEWS_EVENT, mapper.process(List.of("7.01", "8.01", "9.01")));
    }

    // ---- EARNINGS and GOVERNANCE buckets ----------------------------------

    @Test
    void earnings_only_is_classified_as_filing_event() {
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(List.of("2.02", "9.01")));
    }

    @Test
    void governance_only_is_classified_as_filing_event() {
        // 5.02 alone (officer departure) falls into the residual filing-event
        // bucket per the priority ordering.
        assertEquals(CandidateCatalystType.FILING_EVENT, mapper.process(List.of("5.02")));
    }

    // ---- Priority ordering across buckets ---------------------------------

    @Test
    void severe_negative_outranks_governance_when_both_present() {
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("5.02", "4.02")));
    }

    @Test
    void severe_outranks_disclosure_when_both_present() {
        // A 1.03 (bankruptcy) + 8.01 (Other Events) filing is dominated by
        // the bankruptcy — the disclosure is announcing the bankruptcy.
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("1.03", "8.01")));
    }

    @Test
    void mna_outranks_disclosure_when_both_present() {
        assertEquals(CandidateCatalystType.MERGER_AND_ACQUISITION, mapper.process(List.of("2.01", "7.01")));
    }

    @Test
    void dilution_outranks_disclosure_when_both_present() {
        // Capital-structure changes are higher-signal than the announcement
        // disclosure attached to them.
        assertEquals(CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, mapper.process(List.of("3.02", "7.01")));
    }

    @Test
    void disclosure_outranks_earnings_when_both_present() {
        // Earnings releases attach as 2.02 + 9.01 conventionally, not 7.01.
        // If a filing carries both 2.02 and 7.01, the disclosure is something
        // else worth surfacing alongside the earnings.
        assertEquals(CandidateCatalystType.NEWS_EVENT, mapper.process(List.of("2.02", "7.01")));
    }
}
