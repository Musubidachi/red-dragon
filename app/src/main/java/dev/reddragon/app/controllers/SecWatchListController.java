package dev.reddragon.app.controllers;

import dev.reddragon.app.models.PipelineRunResult;
import dev.reddragon.app.services.pipeline.SecWatchListRunner;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/pipeline/sec")
@RequiredArgsConstructor
public class SecWatchListController {

    private static final int MAX_CIKS = 20;
    private final SecWatchListRunner runner;

    @GetMapping("/watch-list")
    public List<PipelineRunResult> runWatchList(
            @RequestParam String ciks,
            @RequestParam(defaultValue = "30") int lookbackDays,
            @RequestParam(defaultValue = "STANDARD") ValidationProfile profile
    ) {
        List<String> cikList = parseCiks(ciks);
        if (cikList.isEmpty()) {
            throw new IllegalArgumentException("At least one CIK is required.");
        }
        return runner.run(cikList, lookbackDays, profile);
    }

    private List<String> parseCiks(String ciks) {
        return Arrays.stream(ciks.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(MAX_CIKS)
                .toList();
    }
}
