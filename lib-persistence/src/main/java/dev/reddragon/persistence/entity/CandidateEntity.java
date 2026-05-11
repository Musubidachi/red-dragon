package dev.reddragon.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "candidate")
public class CandidateEntity {
    @Id
    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "company_name", length = 256)
    private String companyName;

    @Column(name = "catalyst_type", nullable = false, length = 64)
    private String catalystType;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_id", length = 256)
    private String sourceId;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "headline", length = 512)
    private String headline;

    @Column(name = "summary", length = 4000)
    private String summary;

    protected CandidateEntity() {
    }

    public CandidateEntity(
            String candidateId,
            String symbol,
            String companyName,
            String catalystType,
            String sourceType,
            String sourceId,
            String sourceUrl,
            Instant observedAt,
            String headline,
            String summary
    ) {
        this.candidateId = candidateId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.catalystType = catalystType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceUrl = sourceUrl;
        this.observedAt = observedAt;
        this.headline = headline;
        this.summary = summary;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCatalystType() {
        return catalystType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public String getHeadline() {
        return headline;
    }

    public String getSummary() {
        return summary;
    }
}
