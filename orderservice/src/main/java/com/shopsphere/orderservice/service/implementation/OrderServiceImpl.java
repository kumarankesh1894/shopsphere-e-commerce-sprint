package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.dto.AddressDto;
import com.shopsphere.orderservice.dto.OrderItemResponseDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.exception.OrderNotFoundException;
import com.shopsphere.orderservice.repository.OrderRepository;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/*
 * This Service is responsible for fetching order details.
 * It ensures that only the user who placed the order can access its details.
 * It retrieves the order from the database, checks authorization, and
 * maps the Order entity to a OrderResponseDto for the API response.
 * * */

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ModelMapper  modelMapper;

    @Override
    public OrderResponseDto getOrder(Long orderId, Long userId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .address(modelMapper.map(order.getDeliveryAddress(), AddressDto.class))
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponseDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
    }


    /*
     * This method is used to update the status of an existing order.
     *
     * It is primarily called by external services like the Payment Service
     * to reflect changes in the order lifecycle (e.g., PAYMENT_PENDING, PAID, FAILED).
     *
     * Responsibilities:
     * - Fetch the order from the database using orderId
     * - Validate that the order exists
     * - Update the order status based on the input
     * - Persist the updated status in the database
     *
     * Important Notes:
     * - This is an internal operation and should ideally not be exposed to end users
     * - It enables inter-service communication in a microservices architecture
     * - Helps maintain a consistent and centralized order state
     */

    @Override
    public void updateOrderStatus(Long orderId, OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        order.setStatus(status);
        orderRepository.save(order);
    }

    /*
     * This method is responsible for finalizing an order after successful payment.
     *
     * It acts as the final confirmation step in the order lifecycle.
     * The order can only be placed if the payment has been completed successfully.
     *
     * Responsibilities:
     * - Fetch the order using orderId
     * - Verify that the requesting user is authorized (matches userId)
     * - Ensure that the order status is PAID before allowing placement
     * - Update the order status to PACKED/PLACED (based on design)
     * - Set the placedAt timestamp to mark order confirmation
     * - Save the updated order in the database
     *
     * Important Notes:
     * - Prevents users from placing unpaid orders (critical business rule)
     * - Marks the transition from payment stage to fulfillment stage
     * - This is where post-order processes can be triggered (e.g., inventory, notifications)
     */
    @Override
    public void placeOrder(Long orderId, Long userId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order not paid");
        }

        order.setStatus(OrderStatus.PACKED); // or PLACED if you add it
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);
    }
}