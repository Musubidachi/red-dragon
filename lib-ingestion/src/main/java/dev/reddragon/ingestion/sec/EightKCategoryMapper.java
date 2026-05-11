package dev.reddragon.ingestion.sec;

import dev.reddragon.ingestion.model.CandidateCatalystType;

import java.util.List;
import java.util.Set;

/**
 * Maps the numeric Item codes on an 8-K filing to a single
 * {@link CandidateCatalystType}.
 *
 * <p>An 8-K usually carries multiple Item codes. We pick one
 * representative catalyst type based on the highest-signal item present.
 * The priority order encodes how we want to label these filings when the
 * trader sees them.
 */
public class EightKCategoryMapper {

    private static final Set<String> CONTRACT_ITEMS   = Set.of("1.01", "1.02");
    private static final Set<String> SEVERE_ITEMS     = Set.of("1.03", "3.01", "4.02", "2.04");
    private static final Set<String> MNA_ITEMS        = Set.of("2.01", "5.01");
    private static final Set<String> DILUTION_ITEMS   = Set.of("3.02", "3.03");
    private static final Set<String> GOVERNANCE_ITEMS = Set.of("4.01", "5.02", "5.03", "5.07");
    private static final Set<String> EARNINGS_ITEMS   = Set.of("2.02");

    /**
     * Pick the single catalyst label that best describes this 8-K.
     *
     * <p>If no recognised item is present, fall back to a generic
     * filing-event label so downstream code never receives null.
     */
    public CandidateCatalystType process(List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            return CandidateCatalystType.FILING_EVENT;
        }

        if (containsAny(itemCodes, SEVERE_ITEMS))     return CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
        if (containsAny(itemCodes, MNA_ITEMS))        return CandidateCatalystType.CONTRACT;
        if (containsAny(itemCodes, CONTRACT_ITEMS))   return CandidateCatalystType.CONTRACT;
        if (containsAny(itemCodes, DILUTION_ITEMS))   return CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
        if (containsAny(itemCodes, EARNINGS_ITEMS))   return CandidateCatalystType.FILING_EVENT;
        if (containsAny(itemCodes, GOVERNANCE_ITEMS)) return CandidateCatalystType.FILING_EVENT;
        return CandidateCatalystType.FILING_EVENT;
    }

    private boolean containsAny(List<String> itemCodes, Set<String> target) {
        for (String code : itemCodes) {
            if (target.contains(code)) {
                return true;
            }
        }
        return false;
    }
}
