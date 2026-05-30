package dev.reddragon.app.services.review;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.reddragon.app.models.CandidateReviewItem;
import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateReviewService {

    private static final List<String> DEFAULT_VERDICTS = List.of("PASS", "WATCH");

    private final ValidationVerdictRepository verdictRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final TraderNoteRepository traderNoteRepository;
    private final VerdictOverrideRepository overrideRepository;

    public List<CandidateReviewItem> listCandidates(String verdictsParam, int lookbackHours) {
        List<String> verdicts = parseVerdicts(verdictsParam);
        Instant since = Instant.now().minus(Math.max(1, lookbackHours), ChronoUnit.HOURS);

        return verdictRepository
                .findLatestPerCandidateSince(verdicts, since)
                .stream()
                .map(this::toReviewItem)
                .toList();
    }

    private List<String> parseVerdicts(String verdictsParam) {
        if (verdictsParam == null || verdictsParam.isBlank()) {
            return DEFAULT_VERDICTS;
        }
        return Arrays.stream(verdictsParam.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(v -> !v.isBlank())
                .toList();
    }

    private CandidateReviewItem toReviewItem(ValidationVerdictEntity entity) {
        String regimeLabel = analyticsSnapshotRepository
                .findTopByCandidateIdOrderByObservedAtDesc(entity.getCandidateId())
                .map(AnalyticsSnapshotEntity::getRegimeLabel)
                .orElse(null);
        String overrideVerdict = overrideRepository
                .findTopByCandidateIdOrderByOverriddenAtDesc(entity.getCandidateId())
                .map(override -> override.getOverrideVerdict())
                .orElse(null);

        return new CandidateReviewItem(
                entity.getId(),
                entity.getCandidateId(),
                entity.getSymbol(),
                entity.getVerdict(),
                entity.getDeploymentTier(),
                entity.getScore(),
                reasonCodes(entity),
                explanations(entity),
                regimeLabel,
                traderNoteRepository.countByCandidateId(entity.getCandidateId()),
                overrideVerdict,
                entity.getCreatedAt()
        );
    }

    private List<String> reasonCodes(ValidationVerdictEntity entity) {
        if (entity.getReasons() == null || entity.getReasons().isEmpty()) {
            return List.of();
        }
        return entity.getReasons().stream()
                .map(r -> r.getReasonCode())
                .toList();
    }

    private List<String> explanations(ValidationVerdictEntity entity) {
        if (entity.getReasons() == null || entity.getReasons().isEmpty()) {
            return List.of();
        }
        return entity.getReasons().stream()
                .map(r -> r.getExplanation())
                .filter(e -> e != null && !e.isBlank())
                .toList();
    }
}
