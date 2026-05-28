package dev.reddragon.app.config;

import dev.reddragon.analytics.services.MarketStateClassifier;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketAnalysisConfiguration {

    @Bean
    public IntradayStructureSnapshotBuilder intradayStructureSnapshotBuilder() {
        return new IntradayStructureSnapshotBuilder();
    }

    @Bean
    public MarketStateClassifier marketStateClassifier() {
        return new MarketStateClassifier();
    }
}
