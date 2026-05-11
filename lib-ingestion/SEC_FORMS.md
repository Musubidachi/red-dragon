# SEC Forms — Scope and Parsing Notes

Reference for anyone implementing the per-form parsers. For the HTTP layer
that gets you the bytes, see [SEC_API.md](./SEC_API.md). For where the parsed
output goes, see [SEC_IMPLEMENTATION.md](./SEC_IMPLEMENTATION.md).

---

## 1. Forms in scope and why

Order roughly reflects signal density per filing for a catalyst-driven approach.

| Form        | What it is                                                 | Why it matters                                                                             | Implementation difficulty |
| ----------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ------------------------- |
| **8-K**     | Current report — material events                            | Highest signal density. Item codes form a fixed taxonomy → deterministic interpretation.   | Low. Free-text body, but Item codes are structured. |
| **Form 4**  | Insider transaction (officers, directors, 10%+ holders)    | Insider buys are a strong asymmetric signal; cluster buys especially.                      | Medium. Structured XML at `xslF345X03/edgar.xml`. |
| **13D**     | Active 5%+ ownership disclosure                            | Activist intent or strategic stake. Often precedes price re-rating.                        | Medium. HTML body; key fields are extractable. |
| **13G**     | Passive 5%+ ownership disclosure                           | Lower urgency than 13D but still institutional positioning.                                | Medium. Same as 13D. |
| **S-1**     | Initial registration of new securities                     | New issuance → potential dilution.                                                         | Medium. Long HTML; we only need metadata + key terms. |
| **S-3**     | Shelf registration                                          | Established issuer can offer securities later → dilution risk on tap.                      | Medium. Same as S-1. |
| **424B**    | Prospectus (used in connection with offerings)             | The actual offering when an S-3 shelf is exercised. Direct dilution event.                 | Medium. |
| **Form 3**  | Initial statement of beneficial ownership (new insiders)   | Onboarding signal; useful as context, not as a trade trigger.                              | Medium. Same XML family as Form 4. |
| **Form 5**  | Annual catch-up of insider transactions                    | Mostly historical reconciliation. Low signal.                                              | Medium. Same XML family. |

### Out of scope for the first implementation

- **10-K, 10-Q, 20-F** (financial reports). These require XBRL parsing and we don't
  need their content for catalyst detection.
- **13F** (institutional holdings reports). Quarterly, lagged by 45 days; lower
  trading-relevance for short-to-medium horizons.
- **Comment letters, NT-\*** (notification of late filing), and other administrative forms.
- **Form 144** (notice of proposed sale by affiliates) — interesting but noisy.

---

## 2. 8-K Item code taxonomy (actionable subset)

8-K filings are organized by numbered Items. The Item code is the structured
signal — the body is just supporting prose. Map each item to a category so
the analytics layer can score on category, not free text.

| Item    | Title                                                     | Catalyst category       |
| ------- | --------------------------------------------------------- | ----------------------- |
| 1.01    | Entry into a Material Definitive Agreement                | contract / partnership  |
| 1.02    | Termination of a Material Definitive Agreement            | contract / unwind       |
| 1.03    | Bankruptcy or Receivership                                | severe negative         |
| 2.01    | Completion of Acquisition or Disposition of Assets        | M&A                     |
| 2.02    | Results of Operations and Financial Condition             | earnings                |
| 2.03    | Creation of a Direct Financial Obligation (debt)          | balance-sheet event     |
| 2.04    | Triggering Events That Accelerate or Increase Obligations | severe negative         |
| 2.05    | Costs Associated with Exit or Disposal Activities         | restructuring           |
| 2.06    | Material Impairments                                      | negative                |
| 3.01    | Notice of Delisting or Failure to Satisfy a Listing Rule  | severe negative         |
| 3.02    | Unregistered Sales of Equity Securities                   | dilution                |
| 3.03    | Material Modification to Rights of Security Holders       | structural              |
| 4.01    | Changes in Registrant's Certifying Accountant             | governance flag         |
| 4.02    | Non-Reliance on Previously Issued Financial Statements    | severe negative         |
| 5.01    | Changes in Control of Registrant                          | governance / M&A        |
| 5.02    | Departure / Election of Directors / Officers              | governance              |
| 5.03    | Amendments to Articles or Bylaws                          | governance              |
| 5.07    | Submission of Matters to a Vote of Security Holders       | governance              |
| 7.01    | Regulation FD Disclosure                                  | press / disclosure      |
| 8.01    | Other Events                                              | press / disclosure      |
| 9.01    | Financial Statements and Exhibits                         | (always present; ignore as standalone) |

### Notes

- An 8-K usually has multiple items (e.g., `2.02,9.01` for an earnings release
  with the press release attached as exhibit).
- Item 9.01 alone is meaningless — it just declares attached exhibits. Always
  pair it with another Item.
- Items 7.01 and 8.01 are intentionally vague; their value is whether the
  attached press release is material. Worth surfacing the exhibit text.

### Suggested first-pass classification

Reduce each 8-K to one of: `earnings`, `M&A`, `contract`, `governance`,
`severe-negative`, `dilution`, `disclosure`, `other`, based on Item code(s).
Conflicts (e.g., a filing with both `2.02` and `5.02`) get the higher-signal
category; document the tiebreak rules so they're auditable.

---

## 3. Form 4 (insider transactions) — structure

The primary document is XML at the path `xslF345X03/{filing}.xml` within the
filing's archive directory. It conforms to the SEC's
`http://www.sec.gov/edgar/ownership` schema (a.k.a. the "ownership document").

### Key elements to extract

- `issuer/issuerCik`, `issuer/issuerTradingSymbol`
- `reportingOwner/reportingOwnerId/rptOwnerCik`, `rptOwnerName`
- `reportingOwner/reportingOwnerRelationship` — flags `isDirector`, `isOfficer`,
  `officerTitle`, `isTenPercentOwner`
- `nonDerivativeTable/nonDerivativeTransaction[]`:
  - `securityTitle/value` (e.g., "Common Stock")
  - `transactionDate/value`
  - `transactionCoding/transactionCode` (P=purchase, S=sale, A=grant, etc.)
  - `transactionAmounts/transactionShares/value`
  - `transactionAmounts/transactionPricePerShare/value`
  - `transactionAmounts/transactionAcquiredDisposedCode/value` (A or D)
  - `postTransactionAmounts/sharesOwnedFollowingTransaction/value`
- `derivativeTable/derivativeTransaction[]` — options/RSUs; lower trade signal,
  higher noise. Often record-keeping rather than conviction.

### Transaction-code reference (signal-bearing subset)

| Code | Meaning                                                  | Signal?              |
| ---- | -------------------------------------------------------- | -------------------- |
| P    | Open-market purchase                                     | **strong buy signal**|
| S    | Open-market sale                                         | weak (often planned) |
| A    | Grant / award                                            | none (compensation)  |
| M    | Exercise of derivative                                   | weak                 |
| F    | Payment of tax via share withholding                     | none                 |
| G    | Bona fide gift                                           | none                 |
| C    | Conversion of derivative                                 | weak                 |
| X    | Exercise of in-the-money derivative                      | weak                 |

The signal that matters most: **transaction code P, transactionAcquiredDisposedCode
= A**, with a non-trivial dollar amount. Cluster buys (multiple insiders, same
window) are higher signal than singletons.

### Rule 10b5-1 plans

Schedule 10b5-1 plans (pre-arranged sales) are flagged in newer filings via
`<footnote>` references on the relevant transactions. Selling under a 10b5-1
has different signal value than discretionary selling — surface this flag if
extractable, so the analytics layer can discount it.

### Parsing approach

Java's built-in `javax.xml.parsers.DocumentBuilder` + XPath, or JAXB with a
generated model. JAXB is heavier-weight but type-safe; for ~10 elements I'd
go DocumentBuilder + XPath.

---

## 4. 13D / 13G — structure

These are HTML or text bodies, not structured XML. Key fields to extract:

- Filer name and type (institution? activist? individual?)
- Issuer name and CUSIP
- Class of stock
- Aggregate amount beneficially owned (shares)
- Percent of class
- Sole vs shared voting power
- Sole vs shared dispositive power
- Purpose of transaction (Item 4 of the 13D form — free text but often signals intent)

### Parsing approach

Use Jsoup to extract the cover-page table (the form has a canonical cover-page
layout with a fixed table of items). Free-text "Purpose of Transaction" can be
surfaced verbatim with no parsing — let analytics decide what to do with it.

### 13D vs 13G

- **13D** — filed when the holder has any intent to influence control. Triggers
  ongoing amendment obligations on material changes.
- **13G** — passive; the holder commits to no influence-of-control intent.
- **13D/A** — amendment to a previously filed 13D. Same parser.

The activist vs passive distinction is the single most important field. Encode
it explicitly on the parsed payload.

---

## 5. References

- Form 4 / Form 3 / Form 5 XML schema (ownership document):
  <https://www.sec.gov/info/edgar/specifications/ownershipxmltechspec.htm>
- 8-K Item taxonomy (authoritative): <https://www.sec.gov/files/form8-k.pdf>
- Schedule 13D filing rules: <https://www.sec.gov/divisions/corpfin/13d-instructions>
- Daniel Sobrado's `edgar4j` (declined as a dep, but useful for reference,
  particularly Form 4 parsing patterns): <https://github.com/danielsobrado/edgar4j>
