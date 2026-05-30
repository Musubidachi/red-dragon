package dev.reddragon.app.services.review;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.reddragon.app.models.CandidateReviewItem;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateReviewStreamService {

    private static final String DEFAULT_TIMEOUT_MILLIS = "1800000";

    private final CandidateReviewService candidateReviewService;
    private final List<CandidateReviewStreamSubscription> subscriptions = new CopyOnWriteArrayList<>();

    @Value("${red-dragon.review.stream-timeout-ms:" + DEFAULT_TIMEOUT_MILLIS + "}")
    private long streamTimeoutMillis;

    public SseEmitter subscribe(String verdictsParam, int lookbackHours) {
        SseEmitter emitter = new SseEmitter(streamTimeoutMillis);
        CandidateReviewStreamSubscription subscription = new CandidateReviewStreamSubscription(
                emitter,
                verdictsParam,
                Math.max(1, lookbackHours));
        emitter.onCompletion(() -> subscriptions.remove(subscription));
        emitter.onTimeout(() -> subscriptions.remove(subscription));
        emitter.onError(error -> subscriptions.remove(subscription));
        subscriptions.add(subscription);
        sendSnapshot(subscription);
        return emitter;
    }

    @Scheduled(fixedDelayString = "${red-dragon.review.stream-refresh-ms:15000}")
    public void publishSnapshots() {
        for (CandidateReviewStreamSubscription subscription : subscriptions) {
            sendSnapshot(subscription);
        }
    }

    int subscriptionCount() {
        return subscriptions.size();
    }

    private void sendSnapshot(CandidateReviewStreamSubscription subscription) {
        try {
            List<CandidateReviewItem> candidates = candidateReviewService.listCandidates(
                    subscription.verdictsParam(),
                    subscription.lookbackHours());
            subscription.emitter().send(SseEmitter.event()
                    .name("candidates")
                    .data(candidates));
        } catch (IOException | IllegalStateException exception) {
            subscriptions.remove(subscription);
            subscription.emitter().completeWithError(exception);
        }
    }
}
