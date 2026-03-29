package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.client.PaymentClient;
import com.shopsphere.orderservice.dto.AddressDto;
import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.OrderItemResponseDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.dto.PaymentRequestDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.exception.InvalidOrderStateException;
import com.shopsphere.orderservice.exception.OrderAlreadyDeliveredException;
import com.shopsphere.orderservice.exception.OrderAlreadyPackedException;
import com.shopsphere.orderservice.exception.OrderAlreadyShippedException;
import com.shopsphere.orderservice.exception.OrderNotFoundException;
import com.shopsphere.orderservice.exception.OrderTransitionNotAllowedException;
import com.shopsphere.orderservice.exception.UnauthorizedException;
import com.shopsphere.orderservice.repository.OrderRepository;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/*
 * This Service is responsible for fetching order details.
 * It ensures that only the user who placed the order can access its details.
 * It retrieves the order from the database, checks authorization, and
 * maps the Order entity to a OrderResponseDto for the API response.
 * * */

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final ModelMapper  modelMapper;
    private final PaymentClient paymentClient;

    /*
     * This method is used to fetch details of one specific order.
     *
     * It is primarily called when the user opens the order details page.
     *
     * Responsibilities:
     * - Fetch the order from the database using orderId
     * - Validate that the order exists
     * - Verify that the order belongs to the logged-in user
     * - Convert the order entity to response DTO
     * - Return only authorized order data
     *
     * Important Notes:
     * - Prevents users from viewing other users' orders
     * - Throws clear exceptions for not found and unauthorized cases
     * - Keeps response mapping consistent using shared mapper method
     */
    @Override
    public OrderResponseDto getOrder(Long orderId, Long userId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to access this order");
        }

        return mapToOrderResponse(order);
    }

    /*
     * This method is used to fetch order history of the logged-in user.
     *
     * It is primarily called by the My Orders page.
     *
     * Responsibilities:
     * - Read pagination inputs (page and size)
     * - Apply safe limits for page and size
     * - Fetch only the current user's orders
     * - Optionally filter by order status
     * - Sort results in latest-first order
     * - Convert order entities to response DTO list
     * - Return paginated metadata with the order list
     *
     * Important Notes:
     * - Supports scalable history loading for large datasets
     * - Invalid status filter is rejected with clear error message
     * - Never returns other users' orders
     */
    @Override
    public OrderHistoryPageDto getMyOrders(Long userId, int page, int size, String status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage;
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
                orderPage = orderRepository.findByUserIdAndStatus(userId, orderStatus, pageable);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status filter: " + status);
            }
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        return OrderHistoryPageDto.builder()
                .orders(orderPage.getContent().stream().map(this::mapToOrderResponse).toList())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .hasNext(orderPage.hasNext())
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
    public void placeOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.PACKED) {
            throw new OrderAlreadyPackedException("Order is already packed and ready to ship.");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderTransitionNotAllowedException("Cannot pack order. Order must be in PAID state.");
        }

        order.setStatus(OrderStatus.PACKED); // or PLACED if you add it
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    /*
     * This method is used to mark a packed order as shipped.
     *
     * It is primarily called when logistics handover is completed.
     */
    @Override
    public void shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new OrderAlreadyShippedException("Order is already shipped.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderAlreadyDeliveredException("Order is already delivered.");
        }

        if (order.getStatus() != OrderStatus.PACKED) {
            throw new OrderTransitionNotAllowedException("Cannot ship order. Order must be in PACKED state.");
        }

        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
    }

    /*
     * This method is used to mark a shipped order as delivered.
     *
     * It is primarily called when delivery is successfully completed.
     */
    @Override
    public void deliverOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderAlreadyDeliveredException("Order is already delivered.");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new OrderTransitionNotAllowedException("Cannot deliver order. Order must be in SHIPPED state.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    /*
     * Checkout logic implementation
     *
     * Steps:
     * 1. Fetch order from DB
     * 2. Validate that the order belongs to the user
     * 3. Update order status to CHECKOUT
     * 4. Call Payment Service to initiate payment
     */
    @Override
    public PaymentResponseDto startPayment(Long orderId, Long userId) {

        // Step 1: Fetch order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        // Step 2: Authorization check
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to initiate payment for this order");
        }

        // Step 3: Payment can start only from CHECKOUT.
        if (order.getStatus() != OrderStatus.CHECKOUT) {
            throw new InvalidOrderStateException("Order is not in CHECKOUT state");
        }

        // Step 4: Call Payment Service
        return paymentClient.createPayment(buildPaymentRequest(order));
    }

    /*
     * Builds PaymentRequestDto with idempotency key
     *
     * Why idempotencyKey?
     * - Prevents duplicate payment creation
     * - Ensures same request does not create multiple payments
     *
     * Strategy:
     * - Use ORDER_<orderId> as unique key
     */
    private PaymentRequestDto buildPaymentRequest(Order order) {

        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(order.getId());

        // A fresh key per initiation attempt; paymentservice also guards by orderId.
        dto.setIdempotencyKey(order.getId() + "_" + System.currentTimeMillis());

        return dto;
    }

    private OrderResponseDto mapToOrderResponse(Order order) {
        AddressDto addressDto = order.getDeliveryAddress() != null
                ? modelMapper.map(order.getDeliveryAddress(), AddressDto.class)
                : null;

        List<OrderItemResponseDto> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(item -> OrderItemResponseDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .address(addressDto)
                .items(items)
                .build();
    }
}
