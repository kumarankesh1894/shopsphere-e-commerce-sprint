package com.shopsphere.paymentservice.messaging;

import com.shopsphere.paymentservice.config.RabbitMqConfig;
import com.shopsphere.paymentservice.dto.OrderStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderStatusUpdateEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_EVENTS_EXCHANGE,
                RabbitMqConfig.ORDER_STATUS_ROUTING_KEY,
                event
        );
        log.info("payment.rabbitmq.status_event.published orderId={} status={}", event.getOrderId(), event.getStatus());
    }
}

