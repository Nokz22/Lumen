package dev.lumen.config;

import dev.lumen.infrastructure.messaging.RabbitMqDestinations;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology + retry policy for the check-in -> recommendation flow. Queue is bound to a
 * dead-letter exchange so that after the retry policy is exhausted, a poison message
 * lands on the DLQ instead of looping forever or being silently dropped.
 */
@Configuration
public class RabbitMqConfig {

    /**
     * Two retries after the first delivery, so three deliveries in total. Spring Framework 7
     * counts retries rather than attempts, which is one fewer than the number it looks like.
     */
    private static final int MAX_RETRIES = 2;

    private static final int INITIAL_BACKOFF_MS = 500;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final int MAX_BACKOFF_MS = 5000;

    @Bean
    TopicExchange moodCheckInExchange() {
        return new TopicExchange(RabbitMqDestinations.MOOD_CHECK_IN_EXCHANGE);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(RabbitMqDestinations.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Queue moodCheckInQueue() {
        return QueueBuilder.durable(RabbitMqDestinations.MOOD_CHECK_IN_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqDestinations.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqDestinations.MOOD_CHECK_IN_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMqDestinations.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding moodCheckInBinding(Queue moodCheckInQueue, TopicExchange moodCheckInExchange) {
        return BindingBuilder.bind(moodCheckInQueue)
                .to(moodCheckInExchange)
                .with(RabbitMqDestinations.MOOD_CHECK_IN_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitMqDestinations.MOOD_CHECK_IN_ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(RabbitMqDestinations.MOOD_CHECK_IN_EXCHANGE);
        template.setRoutingKey(RabbitMqDestinations.MOOD_CHECK_IN_ROUTING_KEY);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    private StatelessRetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(MAX_RETRIES)
                .backOffOptions(INITIAL_BACKOFF_MS, BACKOFF_MULTIPLIER, MAX_BACKOFF_MS)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
