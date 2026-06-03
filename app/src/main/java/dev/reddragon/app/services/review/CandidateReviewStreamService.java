package dev.reddragon.app.services.review;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.reddragon.app.config.ReviewStreamProperties;
import dev.reddragon.app.models.CandidateReviewItem;
import lombok.RequiredArgsConstructor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class CandidateReviewStreamService {

    private final CandidateReviewService candidateReviewService;
    private final ReviewStreamProperties reviewStreamProperties;
    private final List<CandidateReviewStreamSubscription> subscriptions = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(String verdictsParam, int lookbackHours) {
        SseEmitter emitter = new SseEmitter(reviewStreamProperties.streamTimeoutMs());
        CandidateReviewStreamSubscription[] holder = new CandidateReviewStreamSubscription[1];
        Duration refreshInterval = Duration.ofMillis(reviewStreamProperties.streamRefreshMs());
        Disposable stream = Flux.interval(refreshInterval, refreshInterval)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(tick -> sendSnapshot(holder[0]));

        CandidateReviewStreamSubscription subscription = new CandidateReviewStreamSubscription(
                emitter,
                verdictsParam,
                Math.max(1, lookbackHours),
                stream);
        holder[0] = subscription;

        emitter.onCompletion(() -> dispose(subscription));
        emitter.onTimeout(() -> dispose(subscription));
        emitter.onError(error -> dispose(subscription));
        subscriptions.add(subscription);
        sendSnapshot(subscription);
        return emitter;
    }

    int subscriptionCount() {
        return subscriptions.size();
    }

    private void dispose(CandidateReviewStreamSubscription subscription) {
        subscriptions.remove(subscription);
        subscription.stream().dispose();
    }

    private void sendSnapshot(CandidateReviewStreamSubscription subscription) {
        if (subscription == null) {
            return;
        }
        try {
            List<CandidateReviewItem> candidates = candidateReviewService.listCandidates(
                    subscription.verdictsParam(),
                    subscription.lookbackHours());
            subscription.emitter().send(SseEmitter.event()
                    .name("candidates")
                    .data(candidates));
        } catch (Exception exception) {
            dispose(subscription);
            subscription.emitter().completeWithError(exception);
        }
    }
}
