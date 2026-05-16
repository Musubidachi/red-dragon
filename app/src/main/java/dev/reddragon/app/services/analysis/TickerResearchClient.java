package dev.reddragon.app.services.analysis;

import java.util.List;

import dev.reddragon.app.models.TickerResearchSummary;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;

public interface TickerResearchClient {

    TickerResearchSummary research(
            String ticker,
            MarketQuote quote,
            List<MarketBar> dailyBars,
            List<TradeCandidate> secCandidates
    );
}
