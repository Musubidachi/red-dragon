package dev.reddragon.ingestion.sec;

import java.time.LocalDate;
import java.util.List;

/**
 * Denormalised representation of one SEC filing.
 *
 * <p>This is what the rest of the SEC ingestion pipeline operates on after
 * the column-oriented {@link SubmissionsResponse} is exploded into one row
 * per filing.
 */
public record SecFiling(
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

    /**
     * Reconstructs the canonical URL of this filing's primary document.
     *
     * <p>The Archives endpoint uses a CIK with leading zeros stripped and
     * an accession number with the dashes removed.
     */
    public String primaryDocumentUrl() {
        String cikNoLeadingZeros = String.valueOf(Long.parseLong(cik));
        String accessionNoDashes = accessionNumber.replace("-", "");
        return "https://www.sec.gov/Archives/edgar/data/"
                + cikNoLeadingZeros + "/"
                + accessionNoDashes + "/"
                + primaryDocument;
    }
}
