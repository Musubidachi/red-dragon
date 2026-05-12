package dev.reddragon.app.config;

import dev.reddragon.analytics.service.DeterministicAnalyticsService;
import dev.reddragon.ingestion.sec.EightKCategoryMapper;
import dev.reddragon.ingestion.sec.SecApiProperties;
import dev.reddragon.ingestion.sec.SecCandidateBuilder;
import dev.reddragon.ingestion.sec.SecHttpClient;
import dev.reddragon.ingestion.sec.SecIngestionService;
import dev.reddragon.ingestion.sec.SimpleRateLimiter;
import dev.reddragon.ingestion.sec.SubmissionsClient;
import dev.reddragon.ingestion.sec.SubmissionsFilingExtractor;
import dev.reddragon.ingestion.service.ManualCandidateIngestionService;
import dev.reddragon.marketdata.provider.MarketDataProvider;
import dev.reddragon.marketdata.provider.NoopMarketDataProvider;
import dev.reddragon.marketdata.provider.schwab.SchwabMarketDataProperties;
import dev.reddragon.marketdata.provider.schwab.SchwabMarketDataProvider;
import dev.reddragon.marketdata.service.MarketFeatureCalculator;
import dev.reddragon.persistence.mapper.PersistenceMapper;
import dev.reddragon.validation.engine.DisequilibriumValidationEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PipelineConfiguration {

    @Bean
    public ManualCandidateIngestionService manualCandidateIngestionService() {
        return new ManualCandidateIngestionService();
    }

    @Bean
    public MarketFeatureCalculator marketFeatureCalculator() {
        return new MarketFeatureCalculator();
    }

    @Bean
    public DeterministicAnalyticsService deterministicAnalyticsService() {
        return new DeterministicAnalyticsService();
    }

    @Bean
    public DisequilibriumValidationEngine disequilibriumValidationEngine() {
        return new DisequilibriumValidationEngine();
    }

    @Bean
    public PersistenceMapper persistenceMapper() {
        return new PersistenceMapper();
    }

    @Bean
    public SecApiProperties secApiProperties(
            @Value("${red-dragon.sec.user-agent}") String userAgent,
            @Value("${red-dragon.sec.submissions-base-url}") String submissionsBaseUrl,
            @Value("${red-dragon.sec.requests-per-second}") int requestsPerSecond
    ) {
        return new SecApiProperties(userAgent, submissionsBaseUrl, requestsPerSecond);
    }

    @Bean
    public SecIngestionService secIngestionService(SecApiProperties properties) {
        SimpleRateLimiter limiter = new SimpleRateLimiter(properties.getRequestsPerSecond());
        SecHttpClient http = new SecHttpClient(properties, limiter);
        SubmissionsClient submissions = new SubmissionsClient(properties, http);
        return new SecIngestionService(
                submissions,
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper())
        );
    }

    @Bean
    public SchwabMarketDataProperties schwabMarketDataProperties(
            @Value("${red-dragon.schwab.base-url}") String baseUrl,
            @Value("${red-dragon.schwab.access-token}") String accessToken,
            @Value("${red-dragon.schwab.enabled}") boolean enabled,
            @Value("${red-dragon.schwab.cache-ttl-seconds}") long cacheTtlSeconds
    ) {
        return new SchwabMarketDataProperties(baseUrl, accessToken, enabled, cacheTtlSeconds);
    }

    @Bean
    public MarketDataProvider marketDataProvider(SchwabMarketDataProperties properties) {
        if (properties.configured()) {
            return new SchwabMarketDataProvider(properties);
        }
        return new NoopMarketDataProvider();
    }
}
