package com.shopsphere.orderservice.messaging;

import com.shopsphere.orderservice.config.RabbitMqConfig;
import com.shopsphere.orderservice.dto.OrderStatusUpdateEvent;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_STATUS_QUEUE)
    public void handleOrderStatusUpdate(OrderStatusUpdateEvent event) {
        if (event == null || event.getOrderId() == null || event.getStatus() == null) {
            log.warn("order.rabbitmq.status_event.invalid payload={}", event);
            return;
        }

        try {
            OrderStatus status = OrderStatus.valueOf(event.getStatus().trim().toUpperCase());
            orderService.updateOrderStatus(event.getOrderId(), status);
            log.info("order.rabbitmq.status_event.processed orderId={} status={} source={}",
                    event.getOrderId(), status, event.getSource());
        } catch (IllegalArgumentException ex) {
            log.error("order.rabbitmq.status_event.unknown_status orderId={} status={}",
                    event.getOrderId(), event.getStatus(), ex);
        } catch (Exception ex) {
            log.error("order.rabbitmq.status_event.failed orderId={} status={}",
                    event.getOrderId(), event.getStatus(), ex);
        }
    }
}

