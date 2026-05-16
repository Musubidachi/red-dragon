package dev.reddragon.app.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.TickerAnalysisResponse;
import dev.reddragon.app.services.analysis.TickerAnalysisService;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class TickerAnalysisController {

    private final TickerAnalysisService tickerAnalysisService;

    @GetMapping("/{ticker}")
    public TickerAnalysisResponse analyzeTicker(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "60") int lookbackDays,
            @RequestParam(defaultValue = "STANDARD") ValidationProfile profile
    ) {
        return tickerAnalysisService.analyze(ticker, lookbackDays, profile);
    }
}
