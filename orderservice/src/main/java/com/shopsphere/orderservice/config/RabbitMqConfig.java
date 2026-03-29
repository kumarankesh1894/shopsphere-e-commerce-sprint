package com.shopsphere.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "shopsphere.order.events.exchange";
    public static final String ORDER_STATUS_QUEUE = "shopsphere.order.status.updated.queue";
    public static final String ORDER_STATUS_ROUTING_KEY = "order.status.updated";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderStatusQueue() {
        return new Queue(ORDER_STATUS_QUEUE, true);
    }

    @Bean
    public Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderStatusQueue)
                .to(orderEventsExchange)
                .with(ORDER_STATUS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

