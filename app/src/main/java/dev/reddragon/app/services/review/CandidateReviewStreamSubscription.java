package dev.reddragon.app.services.review;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record CandidateReviewStreamSubscription(
        SseEmitter emitter,
        String verdictsParam,
        int lookbackHours
) {
}
