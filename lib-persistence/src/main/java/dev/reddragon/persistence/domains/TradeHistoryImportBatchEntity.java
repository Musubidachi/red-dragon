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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_history_import_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TradeHistoryImportBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "imported_rows", nullable = false)
    private int importedRows;

    @Column(name = "warnings", length = 8000)
    private String warnings;

    /** DB-populated insertion timestamp (V13); distinct from {@link #importedAt}. */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Backwards-compatible pre-V13 constructor. */
    public TradeHistoryImportBatchEntity(
            Long id,
            Instant importedAt,
            int totalRows,
            int importedRows,
            String warnings
    ) {
        this(id, importedAt, totalRows, importedRows, warnings, null);
    }
}
