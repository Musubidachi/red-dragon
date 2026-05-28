package dev.reddragon.persistence.domains;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedIdEntityBuilderTest {

    private static final List<Class<?>> GENERATED_ID_ENTITIES = List.of(
            AnalyticsSnapshotEntity.class,
            BacktestResultEntity.class,
            CalibrationOutcomeEntity.class,
            IntradayBarEntity.class,
            MarketBarEntity.class,
            MarketQuoteObservationEntity.class,
            MarketSnapshotEntity.class,
            SchwabTokenEntity.class,
            TradeHistoryImportBatchEntity.class,
            TradeHistoryRecordEntity.class,
            TraderNoteEntity.class,
            ValidationVerdictEntity.class,
            ValidationVerdictReasonEntity.class,
            VerdictOverrideEntity.class
    );

    @Test
    void generatedIdEntityBuildersDoNotExposeGeneratedIdSetter() throws ReflectiveOperationException {
        for (Class<?> entityType : GENERATED_ID_ENTITIES) {
            Object builder = entityType.getMethod("builder").invoke(null);
            Set<String> builderMethods = Arrays.stream(builder.getClass().getDeclaredMethods())
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            assertTrue(builderMethods.contains("build"), entityType.getSimpleName() + " builder must build");
            assertFalse(builderMethods.contains("id"),
                    entityType.getSimpleName() + " builder must not accept a generated id");
        }
    }

    @Test
    void generatedIdEntityBuildersCreateTransientEntitiesWithoutIds() throws ReflectiveOperationException {
        for (Class<?> entityType : GENERATED_ID_ENTITIES) {
            Object builder = entityType.getMethod("builder").invoke(null);
            Method build = builder.getClass().getDeclaredMethod("build");
            Object entity = build.invoke(builder);
            Object id = entityType.getMethod("getId").invoke(entity);

            assertNull(id, entityType.getSimpleName() + " builder should leave generated id unset");
        }
    }
}
