package dev.lumen.infrastructure.messaging;

/**
 * Destination names shared between the topology declared in config.RabbitMqConfig and
 * the publisher/listener that use them — kept here (Infrastructure) rather than in
 * Configuration, since Configuration is composition-root-only and nothing else is
 * allowed to depend on it (see LayeredArchitectureTest).
 */
public final class RabbitMqDestinations {

    public static final String MOOD_CHECK_IN_EXCHANGE = "lumen.recommendation";
    public static final String MOOD_CHECK_IN_ROUTING_KEY = "mood-check-in.submitted";
    public static final String MOOD_CHECK_IN_QUEUE = "recommendation.mood-check-in-submitted";
    public static final String DEAD_LETTER_EXCHANGE = "lumen.recommendation.dlx";
    public static final String DEAD_LETTER_QUEUE = "recommendation.mood-check-in-submitted.dlq";

    private RabbitMqDestinations() {
    }
}
