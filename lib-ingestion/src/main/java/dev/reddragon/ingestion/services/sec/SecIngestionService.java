package dev.reddragon.ingestion.services.sec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.SecFiling;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;
import lombok.RequiredArgsConstructor;

/**
 * Top-level entry point for SEC EDGAR ingestion.
 *
 * <p>Given one company CIK, fetches its recent filings, picks the ones we
 * care about, and produces one {@link TradeCandidate} per filing.
 *
 * <p>Composition only — every real piece of work is delegated to a
 * single-responsibility collaborator:
 * <ol>
 *   <li>{@link SubmissionsClient} — fetch the submissions JSON.</li>
 *   <li>{@link SubmissionsFilingExtractor} — flatten and filter to in-scope filings.</li>
 *   <li>{@link SecCandidateBuilder} — turn each filing into a scored candidate.</li>
 * </ol>
 *
 * <p>De-duplication across runs is the caller's responsibility — this
 * service is stateless on purpose.
 */
@RequiredArgsConstructor
public class SecIngestionService {

    private final SubmissionsClient submissionsClient;
    private final SubmissionsFilingExtractor filingExtractor;
    private final SecCandidateBuilder candidateBuilder;

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
