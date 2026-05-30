package dev.reddragon.app.services.schwab;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.reddragon.persistence.domains.BrokerCallLogEntity;
import dev.reddragon.persistence.services.repositories.BrokerCallLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerCallAuditService {

    private final BrokerCallLogRepository brokerCallLogRepository;

    public void record(
            String endpoint,
            Map<String, String> requestBody,
            int responseStatus,
            String responseBody,
            String clientOrderId
    ) {
        brokerCallLogRepository.save(BrokerCallLogEntity.builder()
                .recordedAt(Instant.now())
                .endpoint(require(endpoint, "endpoint"))
                .requestBody(mapToText(requestBody))
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .correlationId(UUID.randomUUID().toString())
                .clientOrderId(clientOrderId)
                .build());
    }

    private String mapToText(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
