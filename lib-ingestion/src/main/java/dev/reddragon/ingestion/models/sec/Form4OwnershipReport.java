package dev.reddragon.ingestion.models.sec;

import java.time.LocalDate;
import java.util.List;

/**
 * Parsed ownership signal from a Form 4 XML primary document.
 *
 * <p>This is intentionally separate from {@link SecFiling}: submissions
 * metadata decides whether a filing is interesting, while this record captures
 * the first body-derived fields the pipeline can reason about later.
 */
public record Form4OwnershipReport(
        String documentType,
        LocalDate periodOfReport,
        String issuerCik,
        String issuerName,
        String issuerTradingSymbol,
        List<Form4ReportingOwner> reportingOwners,
        List<Form4NonDerivativeTransaction> nonDerivativeTransactions
) {

    public Form4OwnershipReport {
        reportingOwners = reportingOwners == null ? List.of() : List.copyOf(reportingOwners);
        nonDerivativeTransactions = nonDerivativeTransactions == null
                ? List.of()
                : List.copyOf(nonDerivativeTransactions);
    }
}
