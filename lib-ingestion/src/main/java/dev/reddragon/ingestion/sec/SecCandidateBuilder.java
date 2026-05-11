package dev.reddragon.ingestion.sec;

import dev.reddragon.ingestion.model.CandidateCatalystType;
import dev.reddragon.ingestion.model.SourceType;
import dev.reddragon.ingestion.model.TradeCandidate;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
 *   by construction. Filings carrying severe-negative or contract items
 *   score the highest.</li>
 *   <li><b>material significance</b>: a placeholder of 0.5 until we have a
 *   fundamentals data source. Form 4 insider purchases tilt this upward.</li>
 *   <li><b>earlyness</b>: derived from the time elapsed since the filing.
 *   Fresh filings are higher.</li>
 *   <li><b>reflexivity potential</b>: a placeholder of 0.5 until we have
 *   news / social ingestion.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class SecCandidateBuilder {

    private final EightKCategoryMapper categoryMapper;
    private final Clock clock;

    public SecCandidateBuilder(EightKCategoryMapper categoryMapper) {
        this(categoryMapper, Clock.systemUTC());
    }

    /**
     * Build a fully populated {@link TradeCandidate} from one filing.
     */
    public TradeCandidate process(SecFiling filing) {
        CandidateCatalystType catalystType = pickCatalystType(filing);
        Instant observedAt = filingDateToInstant(filing.filingDate());

        return new TradeCandidate(
                UUID.randomUUID().toString(),
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
                scoreMaterialSignificance(filing),
                scoreEarlyness(observedAt),
                scoreReflexivityPotential()
        );
    }

    private CandidateCatalystType pickCatalystType(SecFiling filing) {
        return switch (filing.formType()) {
            case "8-K"               -> categoryMapper.process(filing.itemCodes());
            case "4"                 -> CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
            case "SC 13D", "SC 13G"  -> CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
            case "S-1", "S-3", "424B" -> CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE;
            default                  -> CandidateCatalystType.FILING_EVENT;
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

    private double scoreMaterialSignificance(SecFiling filing) {
        if ("4".equals(filing.formType())) {
            return 0.65;
        }
        return 0.50;
    }

    private double scoreEarlyness(Instant observedAt) {
        long hoursSince = Math.max(0, (Instant.now(clock).getEpochSecond() - observedAt.getEpochSecond()) / 3600);
        if (hoursSince < 4)   return 0.90;
        if (hoursSince < 24)  return 0.75;
        if (hoursSince < 72)  return 0.60;
        if (hoursSince < 168) return 0.45;
        return 0.30;
    }

    private double scoreReflexivityPotential() {
        return 0.50;
    }
}
