package dev.reddragon.ingestion.models.sec;

/**
 * Reporting owner identity and relationship flags from a Form 4 filing.
 */
public record Form4ReportingOwner(
        String cik,
        String name,
        boolean director,
        boolean officer,
        boolean tenPercentOwner,
        boolean other,
        String officerTitle
) {
}
