package dev.reddragon.ingestion.services.sec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;

/**
 * Top-level entry point for SEC EDGAR ingestion.
 *
 * <p>Given one company CIK, fetches its recent filings, picks the ones we
 * care about, and produces one {@link TradeCandidate} per filing. When a
 * {@link CikLookupService} is provided, callers can also start from a ticker
 * symbol via {@link #processByTicker(String)}.
 *
 * <p>Composition only; every real piece of work is delegated to a
 * single-responsibility collaborator:
 * <ol>
 *   <li>{@link CikLookupService} - optionally resolve ticker to CIK.</li>
 *   <li>{@link SubmissionsClient} - fetch the submissions JSON.</li>
 *   <li>{@link SubmissionsFilingExtractor} - flatten and filter to in-scope filings.</li>
 *   <li>{@link SecCandidateBuilder} - turn each filing into a scored candidate.</li>
 * </ol>
 *
 * <p>De-duplication across runs is the caller's responsibility; this service
 * is stateless on purpose.
 */
public class SecIngestionService {

    private final SubmissionsClient submissionsClient;
    private final SubmissionsFilingExtractor filingExtractor;
    private final SecCandidateBuilder candidateBuilder;
    private final CikLookupService cikLookupService;

    public SecIngestionService(
            SubmissionsClient submissionsClient,
            SubmissionsFilingExtractor filingExtractor,
            SecCandidateBuilder candidateBuilder
    ) {
        this(submissionsClient, filingExtractor, candidateBuilder, null);
    }

    public SecIngestionService(
            SubmissionsClient submissionsClient,
            SubmissionsFilingExtractor filingExtractor,
            SecCandidateBuilder candidateBuilder,
            CikLookupService cikLookupService
    ) {
        this.submissionsClient = Objects.requireNonNull(submissionsClient, "submissionsClient is required");
        this.filingExtractor = Objects.requireNonNull(filingExtractor, "filingExtractor is required");
        this.candidateBuilder = Objects.requireNonNull(candidateBuilder, "candidateBuilder is required");
        this.cikLookupService = cikLookupService;
    }

    /**
     * Run one ingestion pass for the given CIK and return the resulting
     * candidates in filing order (newest first).
     */
    public List<TradeCandidate> process(String cik) {
        Objects.requireNonNull(cik, "cik is required");

        SubmissionsResponse response = fetchSubmissions(cik);
        List<SecFiling> filings = extractFilings(response);
        return buildCandidates(filings);
    }

    /**
     * Resolve a ticker to CIK, then run the normal CIK-based ingestion path.
     */
    public List<TradeCandidate> processByTicker(String ticker) {
        Objects.requireNonNull(ticker, "ticker is required");
        if (cikLookupService == null) {
            throw new IllegalStateException("CIK lookup service is not configured");
        }
        String cik = cikLookupService.process(ticker)
                .orElseThrow(() -> new IllegalArgumentException("No SEC CIK was found for ticker: " + ticker));
        return process(cik);
    }

    private SubmissionsResponse fetchSubmissions(String cik) {
        return submissionsClient.process(cik);
    }

    private List<SecFiling> extractFilings(SubmissionsResponse response) {
        return filingExtractor.process(response);
    }

    private List<TradeCandidate> buildCandidates(List<SecFiling> filings) {
        List<TradeCandidate> candidates = new ArrayList<>(filings.size());
        for (SecFiling filing : filings) {
            candidates.add(candidateBuilder.process(filing));
        }
        return candidates;
    }
}
