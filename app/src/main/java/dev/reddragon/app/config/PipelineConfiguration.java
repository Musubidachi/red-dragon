package dev.reddragon.app.config;

import dev.reddragon.analytics.service.DeterministicAnalyticsService;
import dev.reddragon.ingestion.service.ManualCandidateIngestionService;
import dev.reddragon.marketdata.service.MarketFeatureCalculator;
import dev.reddragon.validation.engine.DisequilibriumValidationEngine;
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
}
