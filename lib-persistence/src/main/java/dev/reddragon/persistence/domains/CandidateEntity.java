package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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
}
