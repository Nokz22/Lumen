package dev.lumen.infrastructure.messaging;

import dev.lumen.application.recommendation.RecommendationService;
import dev.lumen.domain.recommendation.MoodCheckInSubmittedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Any exception thrown here is handled by the retry interceptor configured on the
 * listener container factory (config.RabbitMqConfig): a few attempts with backoff,
 * then reject-and-don't-requeue routes the message to the dead-letter queue.
 */
@Component
class MoodCheckInEventListener {

    private final RecommendationService recommendationService;

    MoodCheckInEventListener(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @RabbitListener(queues = RabbitMqDestinations.MOOD_CHECK_IN_QUEUE)
    public void onMoodCheckInSubmitted(MoodCheckInSubmittedEvent event) {
        recommendationService.handleMoodCheckInSubmitted(event);
    }
}
