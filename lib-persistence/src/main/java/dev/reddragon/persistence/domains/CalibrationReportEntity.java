package dev.reddragon.persistence.domains;

import java.time.Instant;

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

@Entity
@Table(name = "calibration_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CalibrationReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "drift_level", nullable = false, length = 32)
    private String driftLevel;

    @Column(name = "historical_win_rate", nullable = false)
    private double historicalWinRate;

    @Column(name = "average_return", nullable = false)
    private double averageReturn;

    @Column(name = "average_drawdown", nullable = false)
    private double averageDrawdown;

    @Column(name = "findings")
    private String findings;

    @Column(name = "recommendations")
    private String recommendations;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Builder constructor for new rows; generated IDs and audit columns are database-managed. */
    @Builder
    public CalibrationReportEntity(
            Instant generatedAt,
            String source,
            String driftLevel,
            double historicalWinRate,
            double averageReturn,
            double averageDrawdown,
            String findings,
            String recommendations
    ) {
        this(null, generatedAt, source, driftLevel, historicalWinRate, averageReturn,
                averageDrawdown, findings, recommendations, null);
    }
}
