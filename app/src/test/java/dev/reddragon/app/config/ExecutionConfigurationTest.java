package dev.reddragon.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import dev.reddragon.app.config.ExecutionProperties;
import dev.reddragon.execution.BrokerClient;
import dev.reddragon.execution.DryRunBrokerClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ExecutionProperties.class, () -> new ExecutionProperties(null, false))
            .withUserConfiguration(ExecutionConfiguration.class);

    @Test
    void defaultsToDryRunBrokerClient() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("brokerClient"));
            BrokerClient brokerClient = context.getBean(BrokerClient.class);
            assertTrue(brokerClient instanceof DryRunBrokerClient);
        });
    }
}
