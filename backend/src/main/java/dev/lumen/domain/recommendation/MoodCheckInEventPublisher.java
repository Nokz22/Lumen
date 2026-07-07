package dev.lumen.domain.recommendation;

public interface MoodCheckInEventPublisher {

    void publish(MoodCheckInSubmittedEvent event);
}
