package dev.reddragon.persistence.mapper;

import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.persistence.entity.CandidateEntity;
import dev.reddragon.persistence.entity.ValidationVerdictEntity;
import dev.reddragon.persistence.util.PersistenceStringUtils;
import dev.reddragon.validation.model.ValidationResult;

import java.time.Instant;

/**
 * Maps pipeline domain objects into persistence entities.
 */
public class PersistenceMapper {

    public CandidateEntity toCandidateEntity(TradeCandidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate is required");
        }

        return new CandidateEntity(
                candidate.candidateId(),
                candidate.symbol(),
                candidate.companyName(),
                candidate.catalystType().name(),
                candidate.sourceType().name(),
                candidate.sourceId(),
                candidate.sourceUrl(),
                candidate.observedAt(),
                candidate.headline(),
                candidate.summary()
        );
    }

    public ValidationVerdictEntity toValidationVerdictEntity(ValidationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("validation result is required");
        }

        return new ValidationVerdictEntity(
                null,
                result.candidateId(),
                result.symbol(),
                result.verdict().name(),
                result.deploymentTier().name(),
                result.score(),
                PersistenceStringUtils.joinNames(result.reasonCodes()),
                PersistenceStringUtils.joinText(result.explanations()),
                Instant.now()
        );
    }
}
