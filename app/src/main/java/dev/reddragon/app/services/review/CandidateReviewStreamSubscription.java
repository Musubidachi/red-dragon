package dev.reddragon.app.services.review;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

public record CandidateReviewStreamSubscription(
        SseEmitter emitter,
        String verdictsParam,
        int lookbackHours,
        Disposable stream
) {
}
