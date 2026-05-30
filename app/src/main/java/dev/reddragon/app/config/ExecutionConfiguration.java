package dev.reddragon.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.reddragon.execution.BrokerClient;
import dev.reddragon.execution.DryRunBrokerClient;
import dev.reddragon.execution.ExecutionMode;
import dev.reddragon.execution.LiveBrokerClient;

@Configuration
public class ExecutionConfiguration {

    @Bean
    public ExecutionMode executionMode(
            @Value("${red-dragon.execution.mode:DRY_RUN}") ExecutionMode executionMode
    ) {
        return executionMode;
    }

    @Bean
    public BrokerClient brokerClient(
            ExecutionMode executionMode,
            @Value("${red-dragon.execution.live-enabled:false}") boolean liveEnabled
    ) {
        if (executionMode == ExecutionMode.LIVE) {
            if (!liveEnabled) {
                throw new IllegalStateException(
                        "red-dragon.execution.mode=LIVE requires red-dragon.execution.live-enabled=true");
            }
            return new LiveBrokerClient();
        }
        return new DryRunBrokerClient(
                new dev.reddragon.execution.AccountSummary(
                        "dry-run",
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        false
                ),
                java.util.List.of(),
                java.time.Clock.systemUTC()
        );
    }
}
