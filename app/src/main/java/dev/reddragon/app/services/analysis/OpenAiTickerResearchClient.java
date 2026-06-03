package dev.reddragon.app.services.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.reddragon.app.config.LlmResearchProperties;
import dev.reddragon.app.models.TickerResearchSummary;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

/**
 * OpenAI Responses API client used only for research and explanation.
 */
public class OpenAiTickerResearchClient implements TickerResearchClient {

    private final LlmResearchProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiTickerResearchClient(LlmResearchProperties properties) {
        this(properties, RestClient.create(), new ObjectMapper());
    }

    public OpenAiTickerResearchClient(
            LlmResearchProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public TickerResearchSummary research(
            String ticker,
            MarketQuote quote,
            List<MarketBar> dailyBars,
            List<TradeCandidate> secCandidates
    ) {
        if (!properties.configured()) {
            return TickerResearchSummary.unavailable("LLM research is disabled or not configured.");
        }
        try {
            JsonNode response = postResponse(buildRequest(ticker, quote, dailyBars, secCandidates));
            String text = outputText(response);
            List<String> sources = sources(response);
            return parseSummary(text, sources);
        } catch (RuntimeException error) {
            return TickerResearchSummary.unavailable("LLM research failed: " + error.getMessage());
        }
    }

    private JsonNode postResponse(ObjectNode request) {
        String raw = restClient.post()
                .uri(properties.baseUrl() + "/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request.toString())
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(raw);
        } catch (Exception parseError) {
            throw new IllegalStateException("Failed to parse LLM response", parseError);
        }
    }

    private ObjectNode buildRequest(
            String ticker,
            MarketQuote quote,
            List<MarketBar> dailyBars,
            List<TradeCandidate> secCandidates
    ) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.model());
        request.put("input", prompt(ticker, quote, dailyBars, secCandidates));
        if (properties.webSearchEnabled()) {
            ArrayNode tools = request.putArray("tools");
            tools.addObject().put("type", webSearchToolType());
            request.put("tool_choice", "auto");
            request.putArray("include").add("web_search_call.action.sources");
        }
        return request;
    }

    private String webSearchToolType() {
        if (properties.webSearchToolType() == null || properties.webSearchToolType().isBlank()) {
            return "web_search";
        }
        return properties.webSearchToolType().trim();
    }

    private String prompt(
            String ticker,
            MarketQuote quote,
            List<MarketBar> dailyBars,
            List<TradeCandidate> secCandidates
    ) {
        return """
                You are a market research assistant for a deterministic trade-candidate validator.
                Gather current public information about ticker %s. Use web search if available.

                Return ONLY valid JSON with this shape:
                {
                  "catalystFound": true|false,
                  "catalystType": "GOVERNMENT_GRANT|POLICY_CHANGE|CONTRACT|SUPPLY_CONSTRAINT|SECTOR_INCENTIVE|LIQUIDITY_SHIFT|STRUCTURAL_DEMAND_CHANGE|FILING_EVENT|NEWS_EVENT|SCANNER_EVENT|MANUAL_THESIS",
                  "headline": "short factual catalyst headline",
                  "summary": "concise factual summary with dates",
                  "structuralRealityScore": 0.0,
                  "materialSignificanceScore": 0.0,
                  "earlynessScore": 0.0,
                  "reflexivityPotentialScore": 0.0,
                  "riskFlags": ["..."]
                }

                Rules:
                - Do not decide PASS/WATCH/REJECT.
                - Prefer factual catalysts over sentiment.
                - If no recent factual catalyst is found, set catalystFound=false and use conservative scores.
                - Scores must be between 0 and 1.

                Local context:
                quote=%s
                dailyBarsCount=%d
                recentSecCandidates=%s
                """.formatted(
                ticker.trim().toUpperCase(Locale.ROOT),
                quote,
                dailyBars == null ? 0 : dailyBars.size(),
                summarizeCandidates(secCandidates)
        );
    }

    private String summarizeCandidates(List<TradeCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "[]";
        }
        return candidates.stream()
                .limit(5)
                .map(candidate -> "{type=%s, headline=%s, summary=%s}".formatted(
                        candidate.catalystType(),
                        candidate.headline(),
                        candidate.summary()))
                .toList()
                .toString();
    }

    private String outputText(JsonNode response) {
        StringBuilder text = new StringBuilder();
        JsonNode output = response.path("output");
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            for (JsonNode content : item.path("content")) {
                String value = content.path("text").asText("");
                if (!value.isBlank()) {
                    text.append(value);
                }
            }
        }
        if (text.isEmpty()) {
            return response.path("output_text").asText("");
        }
        return text.toString();
    }

    private List<String> sources(JsonNode response) {
        List<String> values = new ArrayList<>();
        collectUrls(response, values);
        return values.stream().distinct().limit(20).toList();
    }

    private void collectUrls(JsonNode node, List<String> urls) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode url = node.get("url");
            if (url != null && !url.asText("").isBlank()) {
                urls.add(url.asText());
            }
            node.fields().forEachRemaining(entry -> collectUrls(entry.getValue(), urls));
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectUrls(child, urls);
            }
        }
    }

    private TickerResearchSummary parseSummary(String text, List<String> sources) {
        try {
            JsonNode root = objectMapper.readTree(stripJsonFences(text));
            return new TickerResearchSummary(
                    true,
                    root.path("catalystFound").asBoolean(false),
                    catalystType(root.path("catalystType").asText("MANUAL_THESIS")),
                    root.path("headline").asText("LLM research catalyst"),
                    root.path("summary").asText(""),
                    normalized(root.path("structuralRealityScore").asDouble(0.45)),
                    normalized(root.path("materialSignificanceScore").asDouble(0.40)),
                    normalized(root.path("earlynessScore").asDouble(0.50)),
                    normalized(root.path("reflexivityPotentialScore").asDouble(0.35)),
                    stringList(root.path("riskFlags")),
                    sources,
                    text
            );
        } catch (Exception parseError) {
            return new TickerResearchSummary(
                    true,
                    false,
                    CandidateCatalystType.MANUAL_THESIS,
                    "LLM research returned unstructured output",
                    text,
                    0.45,
                    0.40,
                    0.50,
                    0.35,
                    List.of("LLM output was not valid JSON"),
                    sources,
                    text
            );
        }
    }

    private String stripJsonFences(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return value;
    }

    private CandidateCatalystType catalystType(String raw) {
        try {
            return CandidateCatalystType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return CandidateCatalystType.MANUAL_THESIS;
        }
    }

    private double normalized(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private List<String> stringList(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (!item.asText("").isBlank()) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }
}
