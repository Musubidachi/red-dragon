package dev.reddragon.app.controllers;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.reddragon.app.models.CandidateReviewItem;
import dev.reddragon.app.services.review.CandidateReviewService;
import dev.reddragon.app.services.review.CandidateReviewStreamService;
import lombok.RequiredArgsConstructor;

/**
 * Read-only review surface that lists the most recent PASS and WATCH candidates,
 * one row per candidate (de-duplicated to the latest verdict), ordered by score.
 *
 * <p>Example:
 * <pre>
 *   GET /api/review/candidates
 *   GET /api/review/candidates?verdicts=PASS
 *   GET /api/review/candidates?verdicts=PASS,WATCH&lookbackHours=48
 * </pre>
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class CandidateReviewController {

    private static final int DEFAULT_LOOKBACK_HOURS = 24;

    private final CandidateReviewService candidateReviewService;
    private final CandidateReviewStreamService candidateReviewStreamService;

    @GetMapping("/candidates")
    public List<CandidateReviewItem> listCandidates(
            @RequestParam(name = "verdicts", required = false) String verdictsParam,
            @RequestParam(name = "lookbackHours", defaultValue = "24") int lookbackHours
    ) {
        return candidateReviewService.listCandidates(verdictsParam, lookbackHours);
    }

    @GetMapping(path = "/candidates/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCandidates(
            @RequestParam(name = "verdicts", required = false) String verdictsParam,
            @RequestParam(name = "lookbackHours", defaultValue = "" + DEFAULT_LOOKBACK_HOURS) int lookbackHours
    ) {
        return candidateReviewStreamService.subscribe(verdictsParam, lookbackHours);
    }
}
