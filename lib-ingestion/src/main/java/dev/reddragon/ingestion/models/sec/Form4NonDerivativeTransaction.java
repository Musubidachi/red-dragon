package dev.reddragon.ingestion.models.sec;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One non-derivative transaction row from a Form 4 ownership XML document.
 */
public record Form4NonDerivativeTransaction(
        String securityTitle,
        LocalDate transactionDate,
        String transactionCode,
        BigDecimal shares,
        BigDecimal pricePerShare,
        String acquiredDisposedCode,
        BigDecimal sharesOwnedFollowingTransaction,
        String directOrIndirectOwnership
) {
}
