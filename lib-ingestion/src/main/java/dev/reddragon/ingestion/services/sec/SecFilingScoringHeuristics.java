package dev.reddragon.ingestion.services.sec;

import java.util.List;
import java.util.Set;

import dev.reddragon.ingestion.models.sec.SecFiling;

/**
 * Deterministic, transparent scoring of SEC filings on the two dimensions the
 * candidate model still needs after the catalyst type is known:
 * <ul>
 *   <li><b>materiality</b> — how much does this filing actually move the
 *       company's value? Bankruptcies and M&amp;A are high; routine earnings
 *       releases are mid; governance shuffles are low.</li>
 *   <li><b>reflexivity</b> — how likely is this filing to get covered, talked
 *       about, and amplified? M&amp;A and earnings score high; obscure
 *       governance items score low.</li>
 * </ul>
 *
 * <p>This is the explicit replacement for the {@code return 0.50;} placeholders
 * that used to live in {@link SecCandidateBuilder}. Heuristics are tuned to
 * stay strictly inside [0.0, 1.0] and to round to two decimals so the values
 * are easy to read in the review surface.
 *
 * <p>Form-type scoring covers the non-8-K filing types we actually pull
 * (Form 4, SC 13D/G, S-1, S-3, 424B). Anything unknown falls back to a
 * conservative {@code 0.35} so the disequilibrium model doesn't credit it.
 *
 * <p>The class is a pure function. No I/O, no state. Same filing in, same
 * scores out, every time.
 */
public class SecFilingScoringHeuristics {

    // 8-K item-code groups, mirroring the taxonomy used by EightKCategoryMapper.
    // Kept private here so each scoring axis can weight them independently.
    private static final Set<String> SEVERE_ITEMS    = Set.of("1.03", "3.01", "4.02", "2.04");
    private static final Set<String> MNA_ITEMS       = Set.of("2.01", "5.01");
    private static final Set<String> CONTRACT_ITEMS  = Set.of("1.01", "1.02");
    private static final Set<String> DILUTION_ITEMS  = Set.of("3.02", "3.03");
    private static final Set<String> EARNINGS_ITEMS  = Set.of("2.02");
    private static final Set<String> GOVERNANCE_ITEMS = Set.of("4.01", "5.02", "5.03", "5.07");

    /**
     * Score the materiality of a filing on a 0.0–1.0 scale.
     *
     * <p>Materiality answers "does this actually change the company's value?"
     * It is independent of how widely the filing will be discussed.
     */
    public double materiality(SecFiling filing) {
        if (filing == null) {
            return 0.35;
        }

        if ("8-K".equals(filing.formType())) {
            return materialityForEightK(filing.itemCodes());
        }
        return materialityForOtherForm(filing.formType());
    }

    /**
     * Score the reflexivity / attention-grabbing potential of a filing on a
     * 0.0–1.0 scale.
     *
     * <p>Reflexivity is independent of materiality: a routine earnings release
     * always gets coverage even when results are unsurprising; an obscure
     * 5.03 bylaw amendment may be highly material to control but get no air time.
     */
    public double reflexivity(SecFiling filing) {
        if (filing == null) {
            return 0.30;
        }

        if ("8-K".equals(filing.formType())) {
            return reflexivityForEightK(filing.itemCodes());
        }
        return reflexivityForOtherForm(filing.formType());
    }

    // ---- 8-K materiality ----------------------------------------------------

    private double materialityForEightK(List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            return 0.40;
        }
        // Order matters — return the highest-materiality bucket present.
        if (containsAny(itemCodes, SEVERE_ITEMS))     return 0.90; // bankruptcy, restatements
        if (containsAny(itemCodes, MNA_ITEMS))        return 0.85; // acquisitions, change of control
        if (containsAny(itemCodes, CONTRACT_ITEMS))   return 0.75; // material agreements
        if (containsAny(itemCodes, DILUTION_ITEMS))   return 0.65; // unregistered issuance
        if (containsAny(itemCodes, EARNINGS_ITEMS))   return 0.55; // results announcements
        if (containsAny(itemCodes, GOVERNANCE_ITEMS)) return 0.50; // auditor / officer changes
        return 0.40;
    }

    // ---- 8-K reflexivity ----------------------------------------------------

    private double reflexivityForEightK(List<String> itemCodes) {
        if (itemCodes == null || itemCodes.isEmpty()) {
            return 0.30;
        }
        // Order matters — return the highest-attention bucket present.
        if (containsAny(itemCodes, MNA_ITEMS))        return 0.90; // M&A front-page news
        if (containsAny(itemCodes, SEVERE_ITEMS))     return 0.80; // bankruptcy gets press
        if (containsAny(itemCodes, EARNINGS_ITEMS))   return 0.75; // always covered by analysts
        if (containsAny(itemCodes, DILUTION_ITEMS))   return 0.60; // algos react, retail does too
        if (containsAny(itemCodes, CONTRACT_ITEMS))   return 0.55; // depends on size
        if (containsAny(itemCodes, GOVERNANCE_ITEMS)) return 0.40; // depends on prominence
        return 0.30;
    }

    // ---- Non-8-K materiality ------------------------------------------------

    private double materialityForOtherForm(String formType) {
        if (formType == null) {
            return 0.35;
        }
        return switch (formType) {
            // Activist/control filings
            case "SC 13D"                  -> 0.75;
            case "SC 13G"                  -> 0.55;
            // New offerings / dilution
            case "S-1", "S-3", "424B"      -> 0.70;
            // Insider transactions — without size data, mid-range
            case "4"                       -> 0.50;
            default                        -> 0.35;
        };
    }

    // ---- Non-8-K reflexivity ------------------------------------------------

    private double reflexivityForOtherForm(String formType) {
        if (formType == null) {
            return 0.30;
        }
        return switch (formType) {
            case "SC 13D"                  -> 0.75; // activist filings get press
            case "S-1"                     -> 0.65; // IPO filings get press
            case "4"                       -> 0.55; // notable insider trades get noticed
            case "SC 13G"                  -> 0.45;
            case "S-3", "424B"             -> 0.50;
            default                        -> 0.30;
        };
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
