package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.ingestion.models.sec.Form4NonDerivativeTransaction;
import dev.reddragon.ingestion.models.sec.Form4OwnershipReport;
import dev.reddragon.ingestion.models.sec.Form4ReportingOwner;

class Form4OwnershipXmlParserTest {

    private final Form4OwnershipXmlParser parser = new Form4OwnershipXmlParser();

    @Test
    void parsesIssuerOwnerAndNonDerivativeTransactions() throws IOException {
        Form4OwnershipReport report = parser.process(fixture("form4-ownership.xml"));

        assertEquals("4", report.documentType());
        assertEquals(LocalDate.of(2026, 5, 21), report.periodOfReport());
        assertEquals("0000320193", report.issuerCik());
        assertEquals("APPLE INC", report.issuerName());
        assertEquals("AAPL", report.issuerTradingSymbol());

        List<Form4ReportingOwner> owners = report.reportingOwners();
        assertEquals(1, owners.size());
        assertEquals("0001214156", owners.get(0).cik());
        assertEquals("Cook Timothy D", owners.get(0).name());
        assertTrue(owners.get(0).officer());
        assertFalse(owners.get(0).director());
        assertEquals("Chief Executive Officer", owners.get(0).officerTitle());

        List<Form4NonDerivativeTransaction> transactions = report.nonDerivativeTransactions();
        assertEquals(2, transactions.size());
        assertEquals("Common Stock", transactions.get(0).securityTitle());
        assertEquals(LocalDate.of(2026, 5, 21), transactions.get(0).transactionDate());
        assertEquals("S", transactions.get(0).transactionCode());
        assertEquals(new BigDecimal("1000"), transactions.get(0).shares());
        assertEquals(new BigDecimal("195.25"), transactions.get(0).pricePerShare());
        assertEquals("D", transactions.get(0).acquiredDisposedCode());
        assertEquals(new BigDecimal("2990000"), transactions.get(0).sharesOwnedFollowingTransaction());
        assertEquals("D", transactions.get(0).directOrIndirectOwnership());
    }

    @Test
    void allowsTransactionsWithoutPricePerShare() throws IOException {
        Form4OwnershipReport report = parser.process(fixture("form4-gift-ownership.xml"));

        assertEquals(1, report.nonDerivativeTransactions().size());
        Form4NonDerivativeTransaction transaction = report.nonDerivativeTransactions().get(0);
        assertEquals("G", transaction.transactionCode());
        assertEquals(new BigDecimal("250"), transaction.shares());
        assertNull(transaction.pricePerShare());
    }

    @Test
    void rejectsNonForm4OwnershipXml() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ownershipDocument>
                    <documentType>3</documentType>
                </ownershipDocument>
                """;

        assertThrows(IllegalArgumentException.class, () -> parser.process(xml));
    }

    @Test
    void rejectsDoctypeDeclarations() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE ownershipDocument [
                    <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <ownershipDocument>
                    <documentType>4</documentType>
                    <issuer>
                        <issuerName>&xxe;</issuerName>
                    </issuer>
                </ownershipDocument>
                """;

        assertThrows(IllegalArgumentException.class, () -> parser.process(xml));
    }

    private String fixture(String name) throws IOException {
        String path = "/sec/" + name;
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, "Missing test fixture " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
