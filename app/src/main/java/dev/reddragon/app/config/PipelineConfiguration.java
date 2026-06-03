package dev.reddragon.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reddragon.analytics.config.CalibrationDriftThresholds;
import dev.reddragon.analytics.services.exit.EquilibriumCompressionScorer;
import dev.reddragon.analytics.services.meta.LongHorizonCalibrationAnalyzer;
import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.app.services.analysis.NoopTickerResearchClient;
import dev.reddragon.app.services.analysis.OpenAiTickerResearchClient;
import dev.reddragon.app.services.analysis.TickerResearchClient;
import dev.reddragon.app.services.schwab.BrokerCallAuditService;
import dev.reddragon.app.services.schwab.SchwabOAuthService;
import dev.reddragon.backtest.services.BacktestReplayEngine;
import dev.reddragon.ingestion.services.sec.EightKCategoryMapper;
import dev.reddragon.ingestion.services.sec.CikLookupService;
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
import dev.reddragon.marketdata.services.provider.CompositeMarketDataProvider;
import dev.reddragon.marketdata.services.provider.schwab.SchwabAccessTokenSupplier;
import dev.reddragon.marketdata.config.SchwabMarketDataProperties;
import dev.reddragon.marketdata.config.YahooMarketDataProperties;
import dev.reddragon.marketdata.services.provider.schwab.SchwabMarketDataProvider;
import dev.reddragon.marketdata.config.SchwabOAuthProperties;
import dev.reddragon.marketdata.services.provider.schwab.StaticSchwabAccessTokenSupplier;
import dev.reddragon.marketdata.services.provider.yahoo.YahooMarketDataProvider;
import dev.reddragon.analytics.config.MarketScoringProperties;
import dev.reddragon.analytics.services.marketscoring.LiquidityScorer;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.analytics.services.marketscoring.VolatilityStabilityScorer;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.SchwabTokenRepository;
import dev.reddragon.validation.config.ValidationThresholds;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import dev.reddragon.validation.services.ValidationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Default liquidity / volatility scoring thresholds (lib-marketdata
     * REVIEW.md Finding #8 architectural fix). Now lives in
     * {@code lib-analytics.config} — scoring policy belongs at L4, not
     * L2. Replace this bean to retune; defaults match the historical
     * baked-in values so an unconfigured deployment is a no-op.
     */
    @Bean
    public MarketScoringProperties marketScoringProperties() {
        return MarketScoringProperties.defaults();
    }

    @Bean
    public MarketDataSnapshotScorer marketDataSnapshotScorer(
            MarketScoringProperties marketScoringProperties
    ) {
        return new MarketDataSnapshotScorer(
                new LiquidityScorer(marketScoringProperties),
                new VolatilityStabilityScorer(marketScoringProperties));
    }

    @Bean
    public MarketFeatureCalculator marketFeatureCalculator() {
        // Scoring is no longer the marketdata layer's job. The
        // calculator emits a raw snapshot (score=0 placeholders);
        // MarketDataSnapshotScorer enriches it downstream.
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
    public CandidateValidationInputFactory candidateValidationInputFactory() {
        return new CandidateValidationInputFactory();
    }

    @Bean
    public CalibrationDriftThresholds calibrationDriftThresholds(
            @Value("${red-dragon.analytics.calibration.stable-win-rate-min:0.65}") double stableWinRateMin,
            @Value("${red-dragon.analytics.calibration.stable-average-return-min:0.0}") double stableAverageReturnMin,
            @Value("${red-dragon.analytics.calibration.stable-average-drawdown-max:0.15}") double stableAverageDrawdownMax,
            @Value("${red-dragon.analytics.calibration.minor-drift-win-rate-min:0.55}") double minorDriftWinRateMin,
            @Value("${red-dragon.analytics.calibration.minor-drift-average-return-min:0.0}") double minorDriftAverageReturnMin,
            @Value("${red-dragon.analytics.calibration.moderate-drift-win-rate-min:0.45}") double moderateDriftWinRateMin
    ) {
        return new CalibrationDriftThresholds(
                stableWinRateMin,
                stableAverageReturnMin,
                stableAverageDrawdownMax,
                minorDriftWinRateMin,
                minorDriftAverageReturnMin,
                moderateDriftWinRateMin
        );
    }

    @Bean
    public LongHorizonCalibrationAnalyzer longHorizonCalibrationAnalyzer(
            CalibrationDriftThresholds calibrationDriftThresholds
    ) {
        return new LongHorizonCalibrationAnalyzer(calibrationDriftThresholds);
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
            MarketDataSnapshotScorer marketDataSnapshotScorer,
            DeterministicAnalyticsService analyticsService,
            DisequilibriumValidationEngine validationEngine,
            CandidateValidationInputFactory validationInputFactory
    ) {
        return new BacktestReplayEngine(
                marketFeatureCalculator, marketDataSnapshotScorer,
                analyticsService, validationEngine, validationInputFactory);
    }

    @Bean
    public SecApiProperties secApiProperties(
            @Value("${red-dragon.sec.user-agent}") String userAgent,
            @Value("${red-dragon.sec.submissions-base-url}") String submissionsBaseUrl,
            @Value("${red-dragon.sec.company-tickers-url:" + SecApiProperties.DEFAULT_COMPANY_TICKERS_URL + "}") String companyTickersUrl,
            @Value("${red-dragon.sec.requests-per-second}") int requestsPerSecond,
            @Value("${red-dragon.sec.connect-timeout-millis:" + SecApiProperties.DEFAULT_CONNECT_TIMEOUT_MILLIS + "}") int connectTimeoutMillis,
            @Value("${red-dragon.sec.read-timeout-millis:" + SecApiProperties.DEFAULT_READ_TIMEOUT_MILLIS + "}") int readTimeoutMillis,
            @Value("${red-dragon.sec.max-retries:" + SecApiProperties.DEFAULT_MAX_RETRIES + "}") int maxRetries,
            @Value("${red-dragon.sec.backoff-base-millis:" + SecApiProperties.DEFAULT_BACKOFF_BASE_MILLIS + "}") long backoffBaseMillis,
            @Value("${red-dragon.sec.max-backoff-millis:" + SecApiProperties.DEFAULT_MAX_BACKOFF_MILLIS + "}") long maxBackoffMillis,
            @Value("${red-dragon.sec.company-tickers-ttl-millis:" + SecApiProperties.DEFAULT_COMPANY_TICKERS_TTL_MILLIS + "}") long companyTickersTtlMillis
    ) {
        return new SecApiProperties(
                userAgent, submissionsBaseUrl, companyTickersUrl, requestsPerSecond,
                connectTimeoutMillis, readTimeoutMillis,
                maxRetries, backoffBaseMillis, maxBackoffMillis, companyTickersTtlMillis);
    }

    @Bean(destroyMethod = "close")
    public SimpleRateLimiter secRateLimiter(SecApiProperties properties) {
        return new SimpleRateLimiter(properties.getRequestsPerSecond());
    }

    @Bean
    public SecHttpClient secHttpClient(
            SecApiProperties properties,
            SimpleRateLimiter secRateLimiter
    ) {
        return new SecHttpClient(properties, secRateLimiter);
    }

    @Bean
    public SubmissionsClient submissionsClient(
            SecApiProperties properties,
            SecHttpClient secHttpClient,
            ObjectMapper objectMapper
    ) {
        return new SubmissionsClient(properties, secHttpClient, objectMapper);
    }

    @Bean
    public SecIngestionService secIngestionService(
            SubmissionsClient submissions,
            CikLookupService cikLookupService
    ) {
        return new SecIngestionService(
                submissions,
                new SubmissionsFilingExtractor(),
                new SecCandidateBuilder(new EightKCategoryMapper(), new SecFilingScoringHeuristics()),
                cikLookupService
        );
    }

    @Bean
    public CikLookupService cikLookupService(
            SecApiProperties properties,
            SecHttpClient secHttpClient,
            ObjectMapper objectMapper
    ) {
        return new CikLookupService(properties, secHttpClient, objectMapper);
    }

    @Bean
    public TickerResearchClient tickerResearchClient(LlmResearchProperties properties) {
        if (properties.configured()) {
            return new OpenAiTickerResearchClient(properties);
        }
        return new NoopTickerResearchClient();
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
    public YahooMarketDataProperties yahooMarketDataProperties(
            @Value("${red-dragon.yahoo.base-url:https://query1.finance.yahoo.com}") String baseUrl,
            @Value("${red-dragon.yahoo.enabled:false}") boolean enabled,
            @Value("${red-dragon.yahoo.cache-ttl-seconds:60}") long cacheTtlSeconds,
            @Value("${red-dragon.yahoo.max-retries:2}") int maxRetries,
            @Value("${red-dragon.yahoo.retry-backoff-millis:250}") long retryBackoffMillis
    ) {
        return new YahooMarketDataProperties(baseUrl, enabled, cacheTtlSeconds, maxRetries, retryBackoffMillis);
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
            SchwabTokenRepository tokenRepository,
            ObjectProvider<BrokerCallAuditService> brokerCallAuditService
    ) {
        BrokerCallAuditService auditService = brokerCallAuditService.getIfAvailable();
        if (auditService != null) {
            return new SchwabOAuthService(oauthProperties, tokenRepository, RestClient.create(), auditService);
        }
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
            SchwabAccessTokenSupplier tokenSupplier,
            YahooMarketDataProperties yahooProperties
    ) {
        List<MarketDataProvider> providers = new ArrayList<>();
        if (properties.configured() && schwabHasTokenSource(properties, tokenSupplier)) {
            providers.add(new SchwabMarketDataProvider(properties, RestClient.create(), new ObjectMapper(), tokenSupplier));
        }
        if (yahooProperties.configured()) {
            providers.add(new YahooMarketDataProvider(yahooProperties, RestClient.create(), new ObjectMapper()));
        }
        providers.add(new NoopMarketDataProvider());
        return new CompositeMarketDataProvider(providers);
    }

    private boolean schwabHasTokenSource(
            SchwabMarketDataProperties properties,
            SchwabAccessTokenSupplier tokenSupplier
    ) {
        return properties.staticAccessTokenConfigured() || !(tokenSupplier instanceof StaticSchwabAccessTokenSupplier);
    }
}
