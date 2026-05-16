package dev.reddragon.app.services.pipeline;


import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.reddragon.app.models.PipelineRunResult;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecWatchListScheduler {

    private final SecWatchListRunner secWatchListRunner;

    @Value("${red-dragon.scheduler.sec.enabled:true}")
    private boolean enabled;

    @Value("${red-dragon.scheduler.sec.ciks:}")
    private String configuredCiks;

    @Value("${red-dragon.scheduler.sec.lookback-days:30}")
    private int lookbackDays;

    @Value("${red-dragon.scheduler.sec.profile:STANDARD}")
    private ValidationProfile profile;

    @Scheduled(cron = "${red-dragon.scheduler.sec.cron:0 0 13 * * MON-FRI}", zone = "${red-dragon.scheduler.sec.zone:UTC}")
    public void runDailyWatchList() {
        if (!enabled) {
            return;
        }

        List<String> ciks = Arrays.stream(configuredCiks.split(","))
                .map(String::trim)
                .filter(cik -> !cik.isBlank())
                .distinct()
                .toList();

        if (ciks.isEmpty()) {
            log.debug("SEC watch-list scheduler skipped: no CIKs configured.");
            return;
        }

        log.info("Starting scheduled SEC watch-list run for {} CIK(s).", ciks.size());
        List<PipelineRunResult> results = secWatchListRunner.run(ciks, lookbackDays, profile);
        long passed = results.stream().filter(r -> !r.duplicate() && r.validation() != null && r.validation().passed()).count();
        log.info("Finished SEC watch-list run. results={}, passCount={}", results.size(), passed);
    }
}
