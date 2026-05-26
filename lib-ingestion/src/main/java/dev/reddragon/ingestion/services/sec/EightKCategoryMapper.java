package dev.reddragon.ingestion.services.sec;

import dev.reddragon.domain.models.CandidateCatalystType;

import java.util.ArrayList;
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

    // Item-code buckets. Each is mutually exclusive in the priority dispatch;
    // a single 8-K with items spanning multiple buckets returns the highest-
    // priority match per the order documented on process().
    private static final Set<String> CONTRACT_ITEMS   = Set.of("1.01", "1.02");
    // SEVERE expanded to include exit/disposal (2.05) and material impairments
    // (2.06) per SEC_FORMS.md §2 — both reset the thesis on the company.
    private static final Set<String> SEVERE_ITEMS     = Set.of("1.03", "2.04", "2.05", "2.06", "3.01", "4.02");
    private static final Set<String> MNA_ITEMS        = Set.of("2.01", "5.01");
    // DILUTION expanded to include creation of direct financial obligation
    // (2.03) — a capital-structure event in the same family as 3.02/3.03.
    private static final Set<String> DILUTION_ITEMS   = Set.of("2.03", "3.02", "3.03");
    private static final Set<String> GOVERNANCE_ITEMS = Set.of("4.01", "5.02", "5.03", "5.07");
    private static final Set<String> EARNINGS_ITEMS   = Set.of("2.02");
    // DISCLOSURE covers the Reg FD / Other Events items used to attach press
    // releases, FDA announcements, government-contract notices, etc. The
    // attached release is where the signal actually lives; we surface these
    // as NEWS_EVENT so they don't fall through to the generic FILING_EVENT.
    private static final Set<String> DISCLOSURE_ITEMS = Set.of("7.01", "8.01");

    /**
     * Pick the single catalyst label that best describes this 8-K.
     *
     * <p>Item 9.01 (Financial Statements and Exhibits) is stripped first per
     * SEC_FORMS.md §2 Notes — it just declares attached exhibits and carries
     * no standalone signal. If 9.01 was the only item, this falls through to
     * the generic filing-event label.
     *
     * <p>If no recognised item is present, fall back to a generic
     * filing-event label so downstream code never receives null.
     *
     * <h2>Priority order and rationale</h2>
     * Conflicts (a filing with multiple in-scope items) are broken by the
     * order below. The order encodes "highest market-moving impact first"
     * per SEC_FORMS.md §"Suggested first-pass classification":
     *
     * <ol>
     *   <li><b>SEVERE</b> (1.03 bankruptcy, 2.04 acceleration of debt,
     *       2.05 exit/disposal of business, 2.06 material impairment,
     *       3.01 delisting, 4.02 restatement) — these reset the entire thesis
     *       on the company and override any positive item filed alongside.</li>
     *   <li><b>M&amp;A</b> (2.01 acquisition, 5.01 change in control) —
     *       binary, large-impact corporate events. Above contract because
     *       M&amp;A subsumes any underlying material agreement.</li>
     *   <li><b>CONTRACT</b> (1.01 material agreement, 1.02 termination) —
     *       material business news but typically not as catalytic as M&amp;A.</li>
     *   <li><b>DILUTION</b> (2.03 new financial obligation, 3.02 unregistered
     *       sales, 3.03 modification of holders' rights) — capital-structure
     *       change; ranked above earnings because dilution is a one-way signal
     *       whereas earnings can be priced ahead of time.</li>
     *   <li><b>DISCLOSURE</b> (7.01 Reg FD, 8.01 Other Events) — a deliberate
     *       corporate disclosure carrying a press release / regulatory
     *       announcement. Materiality varies by attachment; routed to
     *       {@code NEWS_EVENT} so it surfaces on the review queue rather
     *       than falling through to {@code FILING_EVENT}.</li>
     *   <li><b>EARNINGS</b> (2.02) — material but well-anticipated.</li>
     *   <li><b>GOVERNANCE</b> (4.01 auditor change, 5.02 officer departure,
     *       5.03 charter amendment, 5.07 shareholder vote) — informational;
     *       some 5.02 cases (e.g. CEO ouster) are higher-signal than this
     *       slot suggests, but the framework treats those as outliers worth
     *       trader review rather than auto-promoting.</li>
     * </ol>
     */
    public CandidateCatalystType process(List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            return CandidateCatalystType.FILING_EVENT;
        }

        List<String> filteredCodes = stripExhibitsOnly(itemCodes);
        if (filteredCodes.isEmpty()) {
            return CandidateCatalystType.FILING_EVENT;
        }

        if (containsAny(filteredCodes, SEVERE_ITEMS))     return CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
        if (containsAny(filteredCodes, MNA_ITEMS))        return CandidateCatalystType.MERGER_AND_ACQUISITION;
        if (containsAny(filteredCodes, CONTRACT_ITEMS))   return CandidateCatalystType.CONTRACT;
        if (containsAny(filteredCodes, DILUTION_ITEMS))   return CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
        if (containsAny(filteredCodes, DISCLOSURE_ITEMS)) return CandidateCatalystType.NEWS_EVENT;
        if (containsAny(filteredCodes, EARNINGS_ITEMS))   return CandidateCatalystType.FILING_EVENT;
        if (containsAny(filteredCodes, GOVERNANCE_ITEMS)) return CandidateCatalystType.FILING_EVENT;
        return CandidateCatalystType.FILING_EVENT;
    }

    /**
     * Remove item 9.01 (exhibits-only) from the list. Per SEC_FORMS.md, 9.01
     * alone is meaningless — it just signals that exhibits are attached.
     */
    private List<String> stripExhibitsOnly(List<String> itemCodes) {
        List<String> filtered = new ArrayList<>(itemCodes.size());
        for (String code : itemCodes) {
            if (!"9.01".equals(code)) {
                filtered.add(code);
            }
        }
        return filtered;
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
