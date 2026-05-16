package dev.reddragon.app.controllers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.persistence.domains.IntradayBarEntity;
import dev.reddragon.persistence.domains.MarketBarEntity;
import dev.reddragon.persistence.domains.MarketQuoteObservationEntity;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.IntradayBarRepository;
import dev.reddragon.persistence.services.repositories.MarketBarRepository;
import dev.reddragon.persistence.services.repositories.MarketQuoteObservationRepository;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataProvider marketDataProvider;
    private final MarketBarRepository marketBarRepository;
    private final IntradayBarRepository intradayBarRepository;
    private final MarketQuoteObservationRepository quoteObservationRepository;
    private final PersistenceMapper persistenceMapper;

    @GetMapping("/{symbol}/daily")
    public List<MarketBar> dailyBars(
            @PathVariable String symbol,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        List<MarketBar> bars = marketDataProvider.historicalDailyBars(symbol, start, end);
        persistDailyBars(bars);
        return bars;
    }

    @GetMapping("/{symbol}/daily/stored")
    public List<MarketBarEntity> storedDailyBars(
            @PathVariable String symbol,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        return marketBarRepository.findBySymbolAndBarDateBetweenOrderByBarDateAsc(
                symbol.trim().toUpperCase(), start, end);
    }

    @GetMapping("/{symbol}/intraday")
    public List<IntradayBar> intradayBars(
            @PathVariable String symbol,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "5") long intervalMinutes
    ) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofHours(8)) : from;
        List<IntradayBar> bars = marketDataProvider.intradayBars(symbol, start, end, Duration.ofMinutes(intervalMinutes));
        persistIntradayBars(bars);
        return bars;
    }

    @GetMapping("/{symbol}/intraday/stored")
    public List<IntradayBarEntity> storedIntradayBars(
            @PathVariable String symbol,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(Duration.ofHours(8)) : from;
        return intradayBarRepository.findBySymbolAndStartTimeBetweenOrderByStartTimeAsc(
                symbol.trim().toUpperCase(), start, end);
    }

    @GetMapping("/{symbol}/quote")
    public MarketQuote quote(@PathVariable String symbol) {
        MarketQuote quote = marketDataProvider.quote(symbol);
        quoteObservationRepository.save(persistenceMapper.toMarketQuoteObservationEntity(quote));
        return quote;
    }

    @GetMapping("/{symbol}/quote/stored")
    public List<MarketQuoteObservationEntity> storedQuotes(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "24") int lookbackHours
    ) {
        Instant since = Instant.now().minus(Duration.ofHours(Math.max(1, lookbackHours)));
        return quoteObservationRepository.findBySymbolAndObservedAtAfterOrderByObservedAtDesc(
                symbol.trim().toUpperCase(), since);
    }

    private void persistDailyBars(List<MarketBar> bars) {
        if (bars == null || bars.isEmpty()) {
            return;
        }
        marketBarRepository.saveAll(bars.stream()
                .filter(bar -> !marketBarRepository.existsBySymbolAndBarDate(bar.symbol(), bar.date()))
                .map(persistenceMapper::toMarketBarEntity)
                .toList());
    }

    private void persistIntradayBars(List<IntradayBar> bars) {
        if (bars == null || bars.isEmpty()) {
            return;
        }
        intradayBarRepository.saveAll(bars.stream()
                .filter(bar -> !intradayBarRepository.existsBySymbolAndStartTime(bar.symbol(), bar.startTime()))
                .map(persistenceMapper::toIntradayBarEntity)
                .toList());
    }
}
