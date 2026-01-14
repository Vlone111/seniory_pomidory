package com.marketplace.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.queue.stock-decrease}")
    private String stockDecreaseQueue;

    @Value("${rabbitmq.queue.order-completed}")
    private String orderCompletedQueue;

    @Value("${rabbitmq.routing-key.stock-decrease}")
    private String stockDecreaseRoutingKey;

    @Value("${rabbitmq.routing-key.order-completed}")
    private String orderCompletedRoutingKey;

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange);
    }

    @Bean
    public Queue stockDecreaseQueue() {
        return QueueBuilder.durable(stockDecreaseQueue).build();
    }

    @Bean
    public Queue orderCompletedQueue() {
        return QueueBuilder.durable(orderCompletedQueue).build();
    }

    @Bean
    public Binding stockDecreaseBinding() {
        return BindingBuilder
                .bind(stockDecreaseQueue())
                .to(orderExchange())
                .with(stockDecreaseRoutingKey);
    }

    @Bean
    public Binding orderCompletedBinding() {
        return BindingBuilder
                .bind(orderCompletedQueue())
                .to(orderExchange())
                .with(orderCompletedRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
