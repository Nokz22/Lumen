package dev.lumen.infrastructure.messaging;

import dev.lumen.domain.recommendation.MoodCheckInEventPublisher;
import dev.lumen.domain.recommendation.MoodCheckInSubmittedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * The default exchange/routing key are already bound on the injected RabbitTemplate
 * (see config.RabbitMqConfig), so this class only needs to know how to serialize the
 * event, not where it goes.
 */
@Component
class RabbitMoodCheckInEventPublisher implements MoodCheckInEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    RabbitMoodCheckInEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(MoodCheckInSubmittedEvent event) {
        rabbitTemplate.convertAndSend(event);
    }
}
