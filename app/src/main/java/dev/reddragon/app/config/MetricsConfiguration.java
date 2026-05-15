package dev.reddragon.app.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Enables AspectJ proxies so {@code @Timed} annotations on Spring-managed
 * beans get auto-wired into Micrometer timers exposed at
 * {@code /actuator/metrics}.
 *
 * <p>Without the {@link TimedAspect} bean, {@code @Timed} would be a no-op
 * outside Spring Boot's auto-configured paths (e.g. it works on web
 * controllers via Spring Boot defaults but not on plain {@code @Service}
 * beans).
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfiguration {

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}
