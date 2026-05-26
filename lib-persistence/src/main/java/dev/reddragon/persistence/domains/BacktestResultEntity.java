package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "backtest_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BacktestResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "strategy_name", nullable = false, length = 128)
    private String strategyName;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "verdict", nullable = false, length = 32)
    private String verdict;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "tested_at", nullable = false)
    private Instant testedAt;

    /** DB-populated insertion timestamp (V13); distinct from {@link #testedAt}. */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Backwards-compatible pre-V13 constructor. */
    public BacktestResultEntity(
            Long id,
            String runId,
            String strategyName,
            String symbol,
            String candidateId,
            String verdict,
            double score,
            Instant testedAt
    ) {
        this(id, runId, strategyName, symbol, candidateId, verdict, score, testedAt, null);
    }
}
