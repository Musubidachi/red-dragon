package dev.reddragon.ingestion.services.sec;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.SecFiling;

/**
 * Builds a {@link TradeCandidate} from a denormalised {@link SecFiling}.
 *
 * <p>Each candidate it produces carries four normalised scores. They are
 * intentionally simple and transparent — no machine learning, no opaque
 * transforms. The downstream {@code lib-validation} module is responsible
 * for deciding whether the resulting candidate clears the bar.
 *
 * <p>Scoring rules:
 * <ul>
 *   <li><b>structural reality</b>: SEC filings are factual, so this is high
 *       by construction. Filings carrying severe-negative or contract items
 *       score the highest.</li>
 *   <li><b>material significance</b>: derived from
 *       {@link SecFilingScoringHeuristics#materiality(SecFiling)} using the
 *       8-K item codes or form type — bankruptcies / M&amp;A are high, generic
 *       filings are low.</li>
 *   <li><b>earlyness</b>: derived from the time elapsed since the filing.
 *       Fresh filings are higher.</li>
 *   <li><b>reflexivity potential</b>: derived from
 *       {@link SecFilingScoringHeuristics#reflexivity(SecFiling)} — how
 *       likely the filing is to get covered and amplified.</li>
 * </ul>
 *
 * <p>The candidate's id is the SEC <i>accession number</i>, not a random UUID.
 * This makes ingestion idempotent — the same filing pulled twice produces
 * the same id, and {@code CandidatePipelineOrchestrator}'s dedup check
 * stops it from being re-scored and re-saved.
 */
public class SecCandidateBuilder {

    private final EightKCategoryMapper categoryMapper;
    private final SecFilingScoringHeuristics scoringHeuristics;
    private final Clock clock;

    public SecCandidateBuilder(
            EightKCategoryMapper categoryMapper,
            SecFilingScoringHeuristics scoringHeuristics,
            Clock clock
    ) {
        this.categoryMapper = categoryMapper;
        this.scoringHeuristics = scoringHeuristics;
        this.clock = clock;
    }

    public SecCandidateBuilder(EightKCategoryMapper categoryMapper, SecFilingScoringHeuristics scoringHeuristics) {
        this(categoryMapper, scoringHeuristics, Clock.systemUTC());
    }

    public SecCandidateBuilder(EightKCategoryMapper categoryMapper) {
        this(categoryMapper, new SecFilingScoringHeuristics(), Clock.systemUTC());
    }

    /**
     * Build a fully populated {@link TradeCandidate} from one filing.
     */
    public TradeCandidate process(SecFiling filing) {
        CandidateCatalystType catalystType = pickCatalystType(filing);
        Instant observedAt = filingDateToInstant(filing.filingDate());

        return new TradeCandidate(
                candidateId(filing),
                tickerOrPlaceholder(filing),
                filing.issuerName(),
                catalystType,
                SourceType.SEC_EDGAR,
                filing.accessionNumber(),
                filing.primaryDocumentUrl(),
                observedAt,
                buildHeadline(filing),
                buildSummary(filing),
                scoreStructuralReality(catalystType),
                scoringHeuristics.materiality(filing),
                scoreEarlyness(observedAt),
                scoringHeuristics.reflexivity(filing)
        );
    }

    /**
     * The candidate id is the filing's accession number. Accession numbers
     * are globally unique within SEC EDGAR, so this gives us a natural key:
     * the same filing pulled twice produces the same id and the orchestrator
     * skips the second pass.
     */
    private String candidateId(SecFiling filing) {
        return filing.accessionNumber();
    }

    private CandidateCatalystType pickCatalystType(SecFiling filing) {
        return switch (filing.formType()) {
            case "8-K"                -> categoryMapper.process(filing.itemCodes());
            case "4"                  -> CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
            case "SC 13D", "SC 13G"   -> CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
            case "S-1", "S-3", "424B" -> CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
            default                   -> CandidateCatalystType.FILING_EVENT;
        };
    }

    private String tickerOrPlaceholder(SecFiling filing) {
        return filing.ticker() == null || filing.ticker().isBlank()
                ? "UNKNOWN"
                : filing.ticker();
    }

    private Instant filingDateToInstant(LocalDate filingDate) {
        if (filingDate == null) {
            return Instant.now(clock);
        }
        return filingDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private String buildHeadline(SecFiling filing) {
        return "SEC " + filing.formType() + " — " + filing.issuerName();
    }

    private String buildSummary(SecFiling filing) {
        List<String> items = filing.itemCodes();
        String itemsPart = items == null || items.isEmpty() ? "" : " Items: " + String.join(",", items) + ".";
        String docPart = filing.primaryDocumentDescription() == null || filing.primaryDocumentDescription().isBlank()
                ? ""
                : " " + filing.primaryDocumentDescription() + ".";
        return ("Accession " + filing.accessionNumber() + "." + itemsPart + docPart).trim();
    }

    private double scoreStructuralReality(CandidateCatalystType catalystType) {
        return switch (catalystType) {
            case STRUCTURAL_DEMAND_CHANGE, CONTRACT -> 0.90;
            case FILING_EVENT                       -> 0.75;
            default                                 -> 0.70;
        };
    }

    private double scoreEarlyness(Instant observedAt) {
        long hoursSince = Math.max(0, (Instant.now(clock).getEpochSecond() - observedAt.getEpochSecond()) / 3600);
        if (hoursSince < 4)   return 0.90;
        if (hoursSince < 24)  return 0.75;
        if (hoursSince < 72)  return 0.60;
        if (hoursSince < 168) return 0.45;
        return 0.30;
    }
}
