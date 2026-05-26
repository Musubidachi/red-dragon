package dev.reddragon.ingestion.models.sec;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dev.reddragon.ingestion.services.sec.CikFormats;

/**
 * Denormalised representation of one SEC filing.
 *
 * <p>This is what the rest of the SEC ingestion pipeline operates on after
 * the column-oriented {@link SubmissionsResponse} is exploded into one row
 * per filing.
 *
 * <p><b>{@code acceptanceDateTime}</b> (lib-ingestion REVIEW.md Finding #18)
 * is the wall-clock instant the document arrived at EDGAR (seconds
 * precision). Optional — pre-parsing fixtures and stub responses can pass
 * {@code null}; callers should fall back to {@link #filingDate} in that
 * case. When present it should be preferred over {@code filingDate} for
 * earlyness scoring because the latter anchors at UTC midnight, which
 * is wrong by up to a full day for after-hours filings.
 */
public record SecFiling(
        String cik,
        String issuerName,
        String ticker,
        String accessionNumber,
        String formType,
        LocalDate filingDate,
        Instant acceptanceDateTime,
        String primaryDocument,
        String primaryDocumentDescription,
        List<String> itemCodes
) {

    private static final String ARCHIVES_BASE_URL = "https://www.sec.gov/Archives/edgar/data";

    /**
     * Backwards-compatible nine-arg constructor (pre-Finding-#18 shape).
     * Used by existing tests that don't supply an acceptance timestamp;
     * {@link #acceptanceDateTime} defaults to {@code null} and callers
     * fall back to {@code filingDate} for scoring.
     */
    public SecFiling(
            String cik,
            String issuerName,
            String ticker,
            String accessionNumber,
            String formType,
            LocalDate filingDate,
            String primaryDocument,
            String primaryDocumentDescription,
            List<String> itemCodes
    ) {
        this(cik, issuerName, ticker, accessionNumber, formType, filingDate,
                /* acceptanceDateTime */ null,
                primaryDocument, primaryDocumentDescription, itemCodes);
    }

    /**
     * Reconstructs the canonical URL of this filing's primary document.
     *
     * <p>The Archives endpoint uses a CIK with leading zeros stripped and an
     * accession number with the dashes removed. {@link CikFormats}
     * normalises both forms; the primary-document filename is URL-encoded
     * because SEC document names occasionally contain spaces or accented
     * characters (see lib-ingestion REVIEW.md Finding #16).
     *
     * @return the URL, or {@code null} when {@link #cik()} or
     *         {@link #accessionNumber()} are missing/malformed — callers
     *         that need to surface a link should be ready to handle a
     *         missing one rather than catch a {@link NumberFormatException}
     *         deep in serialisation.
     */
    public String primaryDocumentUrl() {
        if (cik == null || cik.isBlank()) {
            return null;
        }
        if (accessionNumber == null || accessionNumber.isBlank()) {
            return null;
        }
        String cikNoLeadingZeros;
        try {
            cikNoLeadingZeros = CikFormats.stripLeadingZeros(cik);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String accessionNoDashes = CikFormats.accessionNoDashes(accessionNumber);
        String encodedDocument = CikFormats.encodePathSegment(primaryDocument);
        return ARCHIVES_BASE_URL
                + "/" + cikNoLeadingZeros
                + "/" + accessionNoDashes
                + "/" + encodedDocument;
    }
}
