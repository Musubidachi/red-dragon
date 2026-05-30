package dev.reddragon.app.services.marketstate;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketStateSnapshotScheduler {

    private final MarketStateSnapshotRunner runner;

    @Value("${red-dragon.scheduler.market-state.enabled:false}")
    private boolean enabled;

    @Value("${red-dragon.scheduler.market-state.symbols:}")
    private String configuredSymbols;

    @Value("${red-dragon.scheduler.market-state.lookback-hours:8}")
    private int lookbackHours;

    @Value("${red-dragon.scheduler.market-state.interval-minutes:5}")
    private long intervalMinutes;

    @Scheduled(
            cron = "${red-dragon.scheduler.market-state.cron:0 */30 9-16 * * MON-FRI}",
            zone = "${red-dragon.scheduler.market-state.zone:America/New_York}")
    public void runScheduledSnapshots() {
        if (!enabled) {
            return;
        }

        List<String> symbols = Arrays.stream(configuredSymbols.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (symbols.isEmpty()) {
            log.debug("Market-state scheduler skipped: no symbols configured.");
            return;
        }

        for (String symbol : symbols) {
            try {
                runner.run(symbol, lookbackHours, intervalMinutes);
            } catch (Exception exception) {
                log.warn("Market-state snapshot failed for {}: {}", symbol, exception.getMessage());
            }
        }
    }
}
