package dev.reddragon.ingestion.services.sec;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.models.sec.Form4NonDerivativeTransaction;
import dev.reddragon.ingestion.models.sec.Form4OwnershipReport;
import dev.reddragon.ingestion.models.sec.Form4ReportingOwner;
import dev.reddragon.ingestion.models.sec.SecFiling;

/**
 * Opt-in Form 4 body integration.
 *
 * <p>The existing SEC ingestion path builds metadata-level candidates from the
 * submissions feed. This component is deliberately separate: callers choose
 * when to spend a filing-body fetch, and only Form 4 / Form 4-A filings are
 * fetched. It emits one body-derived candidate per filing, keyed separately
 * from the metadata candidate so the two signals can be deduped and compared
 * independently.
 *
 * <p>Source semantics:
 * <ul>
 *   <li>{@code sourceType}: always {@link SourceType#SEC_EDGAR}.</li>
 *   <li>{@code sourceId}: {@code accessionNumber:primaryDocument}, the SEC
 *       document identity that was fetched and parsed.</li>
 *   <li>{@code candidateId}: {@code sec-form4-body:accessionNumber}, stable
 *       across repeated pulls of the same filing body.</li>
 * </ul>
 */
public class SecForm4BodySignalService {

    private static final BigDecimal FIVE_MILLION = BigDecimal.valueOf(5_000_000L);
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);

    private final SecFilingBodyClient bodyClient;
    private final Form4OwnershipXmlParser ownershipParser;
    private final Clock clock;

    public SecForm4BodySignalService(
            SecFilingBodyClient bodyClient,
            Form4OwnershipXmlParser ownershipParser,
            Clock clock
    ) {
        this.bodyClient = Objects.requireNonNull(bodyClient, "bodyClient is required");
        this.ownershipParser = Objects.requireNonNull(ownershipParser, "ownershipParser is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public SecForm4BodySignalService(SecFilingBodyClient bodyClient, Form4OwnershipXmlParser ownershipParser) {
        this(bodyClient, ownershipParser, Clock.systemUTC());
    }

    /**
     * Fetch and parse the primary document for one Form 4 filing.
     *
     * <p>Non-Form-4 filings return {@link Optional#empty()} without touching
     * the network. Parser failures are allowed to propagate so callers can
     * decide whether to retry, quarantine, or surface the bad filing.
     */
    public Optional<TradeCandidate> process(SecFiling filing) {
        Objects.requireNonNull(filing, "filing is required");
        if (!isForm4(filing.formType())) {
            return Optional.empty();
        }

        String body = bodyClient.process(filing);
        Form4OwnershipReport report = ownershipParser.process(body);
        return Optional.of(buildCandidate(filing, report));
    }

    private boolean isForm4(String formType) {
        if (formType == null) {
            return false;
        }
        String normalized = formType.trim();
        return "4".equals(normalized) || "4/A".equals(normalized);
    }

    private TradeCandidate buildCandidate(SecFiling filing, Form4OwnershipReport report) {
        String accessionNumber = requireText(filing.accessionNumber(), "accessionNumber");
        String primaryDocument = requireText(filing.primaryDocument(), "primaryDocument");
        String symbol = firstRequiredText(report.issuerTradingSymbol(), filing.ticker(), "symbol");
        String companyName = firstOptionalText(report.issuerName(), filing.issuerName());
        Instant observedAt = observedAt(filing, report);

        return new TradeCandidate(
                "sec-form4-body:" + accessionNumber,
                symbol,
                companyName,
                CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE,
                SourceType.SEC_EDGAR,
                accessionNumber + ":" + primaryDocument,
                filing.primaryDocumentUrl(),
                observedAt,
                buildHeadline(report, companyName),
                buildSummary(report),
                0.90,
                materialSignificanceScore(report),
                earlynessScore(observedAt),
                reflexivityPotentialScore(report)
        );
    }

    private Instant observedAt(SecFiling filing, Form4OwnershipReport report) {
        if (filing.acceptanceDateTime() != null) {
            return filing.acceptanceDateTime();
        }
        LocalDate observedDate = report.periodOfReport() == null ? filing.filingDate() : report.periodOfReport();
        if (observedDate != null) {
            return observedDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return Instant.now(clock);
    }

    private String buildHeadline(Form4OwnershipReport report, String companyName) {
        String symbol = clean(report.issuerTradingSymbol());
        if (symbol.isBlank()) {
            return "SEC Form 4 ownership activity - " + companyName;
        }
        return "SEC Form 4 ownership activity - " + companyName + " (" + symbol + ")";
    }

    private String buildSummary(Form4OwnershipReport report) {
        List<String> parts = new ArrayList<>();
        String ownerSummary = ownerSummary(report.reportingOwners());
        if (!ownerSummary.isBlank()) {
            parts.add("Owners: " + ownerSummary);
        }
        String transactionSummary = transactionSummary(report.nonDerivativeTransactions());
        if (!transactionSummary.isBlank()) {
            parts.add("Non-derivative transactions: " + transactionSummary);
        }
        if (parts.isEmpty()) {
            return "Form 4 ownership document parsed with no non-derivative transactions.";
        }
        return String.join(". ", parts) + ".";
    }

    private String ownerSummary(List<Form4ReportingOwner> owners) {
        if (owners.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>(owners.size());
        for (Form4ReportingOwner owner : owners) {
            String name = clean(owner.name());
            if (name.isBlank()) {
                continue;
            }
            String relationship = ownerRelationship(owner);
            if (relationship.isBlank()) {
                names.add(name);
            } else {
                names.add(name + " (" + relationship + ")");
            }
        }
        return String.join("; ", names);
    }

    private String ownerRelationship(Form4ReportingOwner owner) {
        List<String> relationships = new ArrayList<>();
        if (owner.director()) {
            relationships.add("director");
        }
        if (owner.officer()) {
            String title = clean(owner.officerTitle());
            relationships.add(title.isBlank() ? "officer" : "officer: " + title);
        }
        if (owner.tenPercentOwner()) {
            relationships.add("10% owner");
        }
        if (owner.other()) {
            relationships.add("other");
        }
        return String.join(", ", relationships);
    }

    private String transactionSummary(List<Form4NonDerivativeTransaction> transactions) {
        if (transactions.isEmpty()) {
            return "";
        }
        List<String> summaries = new ArrayList<>(transactions.size());
        for (Form4NonDerivativeTransaction transaction : transactions) {
            summaries.add(transactionLine(transaction));
        }
        return String.join("; ", summaries);
    }

    private String transactionLine(Form4NonDerivativeTransaction transaction) {
        List<String> parts = new ArrayList<>();
        String code = clean(transaction.transactionCode());
        if (!code.isBlank()) {
            parts.add(code);
        }
        if (transaction.shares() != null) {
            parts.add(transaction.shares().stripTrailingZeros().toPlainString() + " shares");
        }
        if (transaction.pricePerShare() != null) {
            parts.add("at $" + transaction.pricePerShare().stripTrailingZeros().toPlainString());
        }
        if (transaction.transactionDate() != null) {
            parts.add("on " + transaction.transactionDate());
        }
        if (parts.isEmpty()) {
            return "transaction";
        }
        return String.join(" ", parts);
    }

    private double materialSignificanceScore(Form4OwnershipReport report) {
        BigDecimal totalValue = totalTransactionValue(report.nonDerivativeTransactions());
        if (totalValue.compareTo(FIVE_MILLION) >= 0) {
            return 0.70;
        }
        if (totalValue.compareTo(ONE_MILLION) >= 0) {
            return 0.62;
        }
        if (hasOpenMarketTransaction(report.nonDerivativeTransactions())) {
            return 0.55;
        }
        if (!report.nonDerivativeTransactions().isEmpty()) {
            return 0.45;
        }
        return 0.35;
    }

    private BigDecimal totalTransactionValue(List<Form4NonDerivativeTransaction> transactions) {
        BigDecimal total = BigDecimal.ZERO;
        for (Form4NonDerivativeTransaction transaction : transactions) {
            if (transaction.shares() != null && transaction.pricePerShare() != null) {
                total = total.add(transaction.shares().multiply(transaction.pricePerShare()));
            }
        }
        return total;
    }

    private boolean hasOpenMarketTransaction(List<Form4NonDerivativeTransaction> transactions) {
        for (Form4NonDerivativeTransaction transaction : transactions) {
            String code = clean(transaction.transactionCode());
            if ("P".equals(code) || "S".equals(code)) {
                return true;
            }
        }
        return false;
    }

    private double reflexivityPotentialScore(Form4OwnershipReport report) {
        if (hasExecutiveOpenMarketTransaction(report)) {
            return 0.62;
        }
        if (hasDirectorOrOfficer(report.reportingOwners())) {
            return 0.55;
        }
        if (!report.nonDerivativeTransactions().isEmpty()) {
            return 0.45;
        }
        return 0.35;
    }

    private boolean hasExecutiveOpenMarketTransaction(Form4OwnershipReport report) {
        return hasDirectorOrOfficer(report.reportingOwners())
                && hasOpenMarketTransaction(report.nonDerivativeTransactions());
    }

    private boolean hasDirectorOrOfficer(List<Form4ReportingOwner> owners) {
        for (Form4ReportingOwner owner : owners) {
            if (owner.director() || owner.officer() || owner.tenPercentOwner()) {
                return true;
            }
        }
        return false;
    }

    private double earlynessScore(Instant observedAt) {
        long hoursSince = Math.max(0L, (Instant.now(clock).getEpochSecond() - observedAt.getEpochSecond()) / 3600L);
        if (hoursSince < 4L) {
            return 0.90;
        }
        if (hoursSince < 24L) {
            return 0.75;
        }
        if (hoursSince < 72L) {
            return 0.60;
        }
        if (hoursSince < 168L) {
            return 0.45;
        }
        return 0.30;
    }

    private String firstRequiredText(String first, String fallback, String fieldName) {
        String cleaned = clean(first);
        if (!cleaned.isBlank()) {
            return cleaned;
        }
        return requireText(fallback, fieldName);
    }

    private String firstOptionalText(String first, String fallback) {
        String cleaned = clean(first);
        if (!cleaned.isBlank()) {
            return cleaned;
        }
        return clean(fallback);
    }

    private String requireText(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
