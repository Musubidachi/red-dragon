package dev.reddragon.ingestion.services.sec;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import dev.reddragon.ingestion.models.sec.Form4NonDerivativeTransaction;
import dev.reddragon.ingestion.models.sec.Form4OwnershipReport;
import dev.reddragon.ingestion.models.sec.Form4ReportingOwner;

/**
 * Parses SEC Form 4 ownership XML into a small, body-derived model.
 *
 * <p>The first RD-M2 slice only extracts issuer identity, reporting owners,
 * and non-derivative transactions. It does not build candidates or wire body
 * parsing into the live ingestion flow.
 */
public class Form4OwnershipXmlParser {

    public Form4OwnershipReport process(String xml) {
        Objects.requireNonNull(xml, "xml is required");

        Document document = parseDocument(xml);
        Element root = document.getDocumentElement();
        if (root == null || !"ownershipDocument".equals(root.getTagName())) {
            throw new IllegalArgumentException("Expected Form 4 ownershipDocument XML");
        }

        String documentType = directText(root, "documentType");
        if (!isForm4(documentType)) {
            throw new IllegalArgumentException("Expected Form 4 documentType, was: " + documentType);
        }

        Element issuer = firstDirectChild(root, "issuer");
        return new Form4OwnershipReport(
                documentType,
                parseDate(directText(root, "periodOfReport"), "periodOfReport"),
                directText(issuer, "issuerCik"),
                directText(issuer, "issuerName"),
                directText(issuer, "issuerTradingSymbol"),
                reportingOwners(root),
                nonDerivativeTransactions(root)
        );
    }

    private boolean isForm4(String documentType) {
        return "4".equals(documentType) || "4/A".equals(documentType);
    }

    private Document parseDocument(String xml) {
        try {
            DocumentBuilder builder = documentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException parseError) {
            throw new IllegalArgumentException("Failed to parse Form 4 ownership XML", parseError);
        }
    }

    private DocumentBuilder documentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException configurationError) {
            throw new IllegalStateException("Could not configure Form 4 XML parser", configurationError);
        }
    }

    private List<Form4ReportingOwner> reportingOwners(Element root) {
        List<Element> ownerElements = directChildren(root, "reportingOwner");
        List<Form4ReportingOwner> owners = new ArrayList<>(ownerElements.size());
        for (Element owner : ownerElements) {
            Element ownerId = firstDirectChild(owner, "reportingOwnerId");
            Element relationship = firstDirectChild(owner, "reportingOwnerRelationship");
            owners.add(new Form4ReportingOwner(
                    directText(ownerId, "rptOwnerCik"),
                    directText(ownerId, "rptOwnerName"),
                    parseBooleanFlag(directText(relationship, "isDirector")),
                    parseBooleanFlag(directText(relationship, "isOfficer")),
                    parseBooleanFlag(directText(relationship, "isTenPercentOwner")),
                    parseBooleanFlag(directText(relationship, "isOther")),
                    directText(relationship, "officerTitle")
            ));
        }
        return owners;
    }

    private List<Form4NonDerivativeTransaction> nonDerivativeTransactions(Element root) {
        Element table = firstDirectChild(root, "nonDerivativeTable");
        List<Element> transactionElements = directChildren(table, "nonDerivativeTransaction");
        List<Form4NonDerivativeTransaction> transactions = new ArrayList<>(transactionElements.size());
        for (Element transaction : transactionElements) {
            transactions.add(nonDerivativeTransaction(transaction));
        }
        return transactions;
    }

    private Form4NonDerivativeTransaction nonDerivativeTransaction(Element transaction) {
        Element coding = firstDirectChild(transaction, "transactionCoding");
        Element amounts = firstDirectChild(transaction, "transactionAmounts");
        Element postTransactionAmounts = firstDirectChild(transaction, "postTransactionAmounts");
        Element ownershipNature = firstDirectChild(transaction, "ownershipNature");

        return new Form4NonDerivativeTransaction(
                valueText(transaction, "securityTitle"),
                parseDate(valueText(transaction, "transactionDate"), "transactionDate"),
                directText(coding, "transactionCode"),
                parseDecimal(valueText(amounts, "transactionShares"), "transactionShares"),
                parseDecimal(valueText(amounts, "transactionPricePerShare"), "transactionPricePerShare"),
                valueText(amounts, "transactionAcquiredDisposedCode"),
                parseDecimal(valueText(postTransactionAmounts, "sharesOwnedFollowingTransaction"),
                        "sharesOwnedFollowingTransaction"),
                valueText(ownershipNature, "directOrIndirectOwnership")
        );
    }

    private boolean parseBooleanFlag(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException parseError) {
            throw new IllegalArgumentException("Invalid Form 4 date in " + fieldName + ": " + value, parseError);
        }
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException parseError) {
            throw new IllegalArgumentException("Invalid Form 4 decimal in " + fieldName + ": " + value, parseError);
        }
    }

    private String valueText(Element parent, String wrapperTag) {
        Element wrapper = firstDirectChild(parent, wrapperTag);
        return directText(wrapper, "value");
    }

    private String directText(Element parent, String tagName) {
        Element child = firstDirectChild(parent, tagName);
        if (child == null) {
            return null;
        }
        String text = child.getTextContent();
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Element firstDirectChild(Element parent, String tagName) {
        List<Element> children = directChildren(parent, tagName);
        if (children.isEmpty()) {
            return null;
        }
        return children.get(0);
    }

    private List<Element> directChildren(Element parent, String tagName) {
        if (parent == null) {
            return List.of();
        }
        NodeList nodes = parent.getChildNodes();
        List<Element> matches = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                matches.add(element);
            }
        }
        return matches;
    }
}
