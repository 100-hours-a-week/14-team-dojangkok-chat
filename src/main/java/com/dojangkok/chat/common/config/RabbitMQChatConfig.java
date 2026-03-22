package com.dojangkok.chat.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class RabbitMQChatConfig {

    public static final String NOTIFICATION_QUEUE = "quorum.notification";

    public static final String CHAT_FANOUT_EXCHANGE = "chat.fanout";
    public static final String NOTIFICATION_EXCHANGE = "notification.events";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    @Bean
    public FanoutExchange chatFanoutExchange() {
        return new FanoutExchange(CHAT_FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatInstanceQueue() {
        String queueName = "chat.instance." + UUID.randomUUID();
        return new Queue(queueName, false, true, true);
    }

    @Bean
    public Binding chatQueueBinding(Queue chatInstanceQueue, FanoutExchange chatFanoutExchange) {
        return BindingBuilder.bind(chatInstanceQueue).to(chatFanoutExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey("quorum.notification")
                .ttl(300000)
                .quorum()
                .build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange).with("chat.notification");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
