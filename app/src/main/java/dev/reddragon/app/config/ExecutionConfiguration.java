package dev.reddragon.app.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.reddragon.execution.AccountSummary;
import dev.reddragon.execution.BrokerClient;
import dev.reddragon.execution.DryRunBrokerClient;
import dev.reddragon.execution.ExecutionMode;
import dev.reddragon.execution.LiveBrokerClient;

@Configuration
public class ExecutionConfiguration {

    @Bean
    public ExecutionMode executionMode(ExecutionProperties executionProperties) {
        return executionProperties.mode();
    }

    @Bean
    public BrokerClient brokerClient(ExecutionProperties executionProperties, ExecutionMode executionMode) {
        if (executionMode == ExecutionMode.LIVE) {
            if (!executionProperties.liveEnabled()) {
                throw new IllegalStateException(
                        "Live execution is disabled. Set red-dragon.execution.live-enabled=true "
                                + "before enabling red-dragon.execution.mode=LIVE.");
            }
            return new LiveBrokerClient();
        }
        return new DryRunBrokerClient(defaultAccountSummary(), List.of(), Clock.systemUTC());
    }

    private AccountSummary defaultAccountSummary() {
        return new AccountSummary(
                "DRY-RUN",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false
        );
    }
}
