package dev.reddragon.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reddragon.analytics.services.exit.EquilibriumCompressionScorer;
import dev.reddragon.analytics.services.meta.LongHorizonCalibrationAnalyzer;
import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.app.services.schwab.SchwabOAuthService;
import dev.reddragon.backtest.services.BacktestReplayEngine;
import dev.reddragon.ingestion.services.sec.EightKCategoryMapper;
import dev.reddragon.ingestion.config.SecApiProperties;
import dev.reddragon.ingestion.services.sec.SecCandidateBuilder;
import dev.reddragon.ingestion.services.sec.SecFilingScoringHeuristics;
import dev.reddragon.ingestion.services.sec.SecHttpClient;
import dev.reddragon.ingestion.services.sec.SecIngestionService;
import dev.reddragon.ingestion.services.sec.SimpleRateLimiter;
import dev.reddragon.ingestion.services.sec.SubmissionsClient;
import dev.reddragon.ingestion.services.sec.SubmissionsFilingExtractor;
import dev.reddragon.ingestion.services.ManualCandidateIngestionService;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import dev.reddragon.marketdata.services.provider.NoopMarketDataProvider;
import dev.reddragon.marketdata.services.provider.schwab.SchwabAccessTokenSupplier;
import dev.reddragon.marketdata.config.SchwabMarketDataProperties;
import dev.reddragon.marketdata.services.provider.schwab.SchwabMarketDataProvider;
import dev.reddragon.marketdata.config.SchwabOAuthProperties;
import dev.reddragon.marketdata.services.provider.schwab.StaticSchwabAccessTokenSupplier;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.SchwabTokenRepository;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.services.ValidationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Top-level wiring for the candidate pipeline.
 *
 * <p>Beans here are deliberately constructed by hand so the dependency graph
 * between modules stays explicit and visible. The orchestration services
 * (CandidatePipelineOrchestrator, SecWatchListRunner, etc.) get their
 * collaborators from this configuration.
 */
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
    public DisequilibriumValidationEngine disequilibriumValidationEngine(
            ValidationThresholds defaultThresholds
    ) {
        return new DisequilibriumValidationEngine(defaultThresholds);
    }

    /**
     * ValidationService is the stable facade for the validation subsystem.
     * It wraps DisequilibriumValidationEngine and also resolves RiskFlags
     * and ValidationConfidenceScore — use this in preference to the engine directly.
     */
    @Bean
    public ValidationService validationService(ValidationThresholds defaultThresholds) {
        return new ValidationService(defaultThresholds);
    }

    @Bean
    public LongHorizonCalibrationAnalyzer longHorizonCalibrationAnalyzer() {
        return new LongHorizonCalibrationAnalyzer();
    }

    @Bean
    public EquilibriumCompressionScorer equilibriumCompressionScorer() {
        return new EquilibriumCompressionScorer();
    }

    @Bean
    public PersistenceMapper persistenceMapper() {
        return new PersistenceMapper();
    }

    @Bean
    public BacktestReplayEngine backtestReplayEngine(
            MarketFeatureCalculator marketFeatureCalculator,
            DeterministicAnalyticsService analyticsService,
            DisequilibriumValidationEngine validationEngine
    ) {
        return new BacktestReplayEngine(marketFeatureCalculator, analyticsService, validationEngine);
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
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics())
        );
    }

    @Bean
    public SchwabMarketDataProperties schwabMarketDataProperties(
            @Value("${red-dragon.schwab.base-url}") String baseUrl,
            @Value("${red-dragon.schwab.access-token}") String accessToken,
            @Value("${red-dragon.schwab.enabled}") boolean enabled,
            @Value("${red-dragon.schwab.cache-ttl-seconds}") long cacheTtlSeconds,
            @Value("${red-dragon.schwab.max-retries:3}") int maxRetries,
            @Value("${red-dragon.schwab.retry-backoff-millis:250}") long retryBackoffMillis
    ) {
        return new SchwabMarketDataProperties(baseUrl, accessToken, enabled, cacheTtlSeconds, maxRetries, retryBackoffMillis);
    }

    @Bean
    public SchwabOAuthProperties schwabOAuthProperties(
            @Value("${red-dragon.schwab.client-id:}") String clientId,
            @Value("${red-dragon.schwab.client-secret:}") String clientSecret,
            @Value("${red-dragon.schwab.refresh-token:}") String refreshToken,
            @Value("${red-dragon.schwab.redirect-uri:https://127.0.0.1:8080/api/schwab/oauth/callback}") String redirectUri,
            @Value("${red-dragon.schwab.token-url:https://api.schwabapi.com/v1/oauth/token}") String tokenUrl
    ) {
        return new SchwabOAuthProperties(clientId, clientSecret, refreshToken, redirectUri, tokenUrl);
    }

    /**
     * Created only when OAuth is fully configured (client id + secret + token URL).
     * When this bean is absent, SchwabTokenRefresher and SchwabOAuthController
     * both back off via @ConditionalOnBean and the market-data provider falls
     * back to the static access-token supplier.
     */
    @Bean
    @Conditional(SchwabOAuthConfiguredCondition.class)
    public SchwabOAuthService schwabOAuthService(
            SchwabOAuthProperties oauthProperties,
            SchwabTokenRepository tokenRepository
    ) {
        return new SchwabOAuthService(oauthProperties, tokenRepository, RestClient.create());
    }

    @Bean
    public SchwabAccessTokenSupplier schwabAccessTokenSupplier(
            SchwabMarketDataProperties properties,
            ObjectProvider<SchwabOAuthService> oauthService
    ) {
        SchwabOAuthService configured = oauthService.getIfAvailable();
        if (configured != null) {
            return configured;
        }
        return new StaticSchwabAccessTokenSupplier(properties);
    }

    @Bean
    public MarketDataProvider marketDataProvider(
            SchwabMarketDataProperties properties,
            SchwabAccessTokenSupplier tokenSupplier
    ) {
        if (properties.configured()) {
            return new SchwabMarketDataProvider(properties, RestClient.create(), new ObjectMapper(), tokenSupplier);
        }
        return new NoopMarketDataProvider();
    }
}
