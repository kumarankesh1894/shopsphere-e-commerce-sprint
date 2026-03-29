package com.shopsphere.orderservice.service.implementation;

import com.shopsphere.orderservice.client.CatalogServiceClient;
import com.shopsphere.orderservice.client.PaymentClient;
import com.shopsphere.orderservice.dto.AddressDto;
import com.shopsphere.orderservice.dto.OrderAdminDto;
import com.shopsphere.orderservice.dto.OrderAdminItemDto;
import com.shopsphere.orderservice.dto.OrderHistoryPageDto;
import com.shopsphere.orderservice.dto.OrderItemResponseDto;
import com.shopsphere.orderservice.dto.OrderResponseDto;
import com.shopsphere.orderservice.dto.PaymentRequestDto;
import com.shopsphere.orderservice.dto.PaymentResponseDto;
import com.shopsphere.orderservice.entity.Order;
import com.shopsphere.orderservice.enums.OrderStatus;
import com.shopsphere.orderservice.exception.InvalidOrderStateException;
import com.shopsphere.orderservice.exception.OrderAlreadyCancelledException;
import com.shopsphere.orderservice.exception.OrderAlreadyDeliveredException;
import com.shopsphere.orderservice.exception.OrderAlreadyPackedException;
import com.shopsphere.orderservice.exception.OrderAlreadyShippedException;
import com.shopsphere.orderservice.exception.OrderCancellationNotAllowedException;
import com.shopsphere.orderservice.exception.OrderNotFoundException;
import com.shopsphere.orderservice.exception.OrderTransitionNotAllowedException;
import com.shopsphere.orderservice.exception.UnauthorizedException;
import com.shopsphere.orderservice.repository.OrderRepository;
import com.shopsphere.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final ModelMapper  modelMapper;
    private final PaymentClient paymentClient;
    private final CatalogServiceClient catalogClient;

    // =============================
    // User APIs
    // =============================

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
    @Cacheable(value = "orderByUser", key = "#orderId + '-' + #userId")
    public OrderResponseDto getOrder(Long orderId, Long userId) {
        log.info("order.get.start orderId={} userId={}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
                log.warn("order.get.unauthorized orderId={} requestedBy={} owner={}",
                    orderId, userId, order.getUserId());
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
    @Cacheable(value = "orderHistory", key = "#userId + '-' + #page + '-' + #size + '-' + (#status == null ? 'ALL' : #status)")
    public OrderHistoryPageDto getMyOrders(Long userId, int page, int size, String status) {
        log.info("order.history.start userId={} page={} size={} status={}", userId, page, size, status);
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
                log.warn("order.history.invalid_filter userId={} status={}", userId, status);
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


    // =============================
    // Admin APIs
    // =============================

    /*
     * This method is responsible for finalizing an order after successful payment.
     *
     * It acts as the admin packing step in the order lifecycle.
     *
     * Responsibilities:
     * - Fetch order by orderId
     * - Validate current state is PAID
     * - Prevent duplicate pack action
     * - Move status to PACKED
     * - Store packedAt audit timestamp
     */
    @Override
    @CacheEvict(value = {"orderByUser", "orderHistory", "orderInternal"}, allEntries = true)
    public void placeOrder(Long orderId) {
        log.info("order.lifecycle.pack.start orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.PACKED) {
            throw new OrderAlreadyPackedException("Order is already packed and ready to ship.");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderTransitionNotAllowedException("Cannot pack order. Order must be in PAID state.");
        }

        order.setStatus(OrderStatus.PACKED); // or PLACED if you add it
        order.setPackedAt(LocalDateTime.now());

        orderRepository.save(order);
        log.info("order.lifecycle.pack.success orderId={} newStatus=PACKED", orderId);
    }

    /*
     * This method marks a packed order as shipped.
     *
     * Responsibilities:
     * - Fetch order by orderId
     * - Validate current state is PACKED
     * - Prevent duplicate/invalid ship action
     * - Move status to SHIPPED
     * - Store shippedAt audit timestamp
     */
    @Override
    @CacheEvict(value = {"orderByUser", "orderHistory", "orderInternal"}, allEntries = true)
    public void shipOrder(Long orderId) {
        log.info("order.lifecycle.ship.start orderId={}", orderId);
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
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("order.lifecycle.ship.success orderId={} newStatus=SHIPPED", orderId);
    }

    /*
     * This method marks a shipped order as delivered.
     *
     * Responsibilities:
     * - Fetch order by orderId
     * - Validate current state is SHIPPED
     * - Prevent duplicate deliver action
     * - Move status to DELIVERED
     * - Store deliveredAt audit timestamp
     */
    @Override
    @CacheEvict(value = {"orderByUser", "orderHistory", "orderInternal"}, allEntries = true)
    public void deliverOrder(Long orderId) {
        log.info("order.lifecycle.deliver.start orderId={}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderAlreadyDeliveredException("Order is already delivered.");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new OrderTransitionNotAllowedException("Cannot deliver order. Order must be in SHIPPED state.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("order.lifecycle.deliver.success orderId={} newStatus=DELIVERED", orderId);
    }

    /*
     * This method handles cancellation by the order owner (user flow).
     *
     * Responsibilities:
     * - Fetch order and validate ownership
     * - Block cancellation for delivered orders
     * - Block duplicate cancellation
     * - Apply cancellation status and timestamp
     */
    @Override
    @CacheEvict(value = {"orderByUser", "orderHistory", "orderInternal"}, allEntries = true)
    public void cancelOrder(Long orderId, Long userId) {
        log.info("order.cancel.user.start orderId={} userId={}", orderId, userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            log.warn("order.cancel.user.unauthorized orderId={} requestedBy={} owner={}",
                    orderId, userId, order.getUserId());
            throw new UnauthorizedException("You are not allowed to cancel this order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException("Order is already cancelled.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancellationNotAllowedException("Cannot cancel order. Order is already delivered.");
        }

        applyCancellation(order);
    }

    /*
     * This method handles cancellation by admin.
     *
     * Responsibilities:
     * - Fetch order without ownership restriction
     * - Block cancellation for delivered orders
     * - Block duplicate cancellation
     * - Apply cancellation status and timestamp
     */
    @Override
    @CacheEvict(value = {"orderByUser", "orderHistory", "orderInternal"}, allEntries = true)
    public void cancelOrderAsAdmin(Long orderId) {
        log.info("order.cancel.admin.start orderId={}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException("Order is already cancelled.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancellationNotAllowedException("Cannot cancel order. Order is already delivered.");
        }

        applyCancellation(order);
    }

    // =============================
    // Internal service-to-service APIs
    // =============================

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
    @CacheEvict(value = {"orderByUser", "orderHistory", "orderInternal"}, allEntries = true)
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("order.status.update.start orderId={} targetStatus={}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        OrderStatus previousStatus = order.getStatus();

        // Reduce inventory only once when payment is confirmed.
        if (status == OrderStatus.PAID && order.getStatus() != OrderStatus.PAID) {
            reduceStockForOrder(order);
        }

        order.setStatus(status);
        orderRepository.save(order);
        log.info("order.status.update.success orderId={} previousStatus={} newStatus={}", orderId, previousStatus, status);
    }

    /*
     * Helper method:
     * Applies common cancellation updates on the order.
     */
    private void applyCancellation(Order order) {
        log.info("order.cancel.apply.start orderId={}", order.getId());
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("order.cancel.apply.success orderId={} newStatus=CANCELLED", order.getId());
    }


    private void reduceStockForOrder(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("order.stock_reduce.skipped orderId={} reason=no_items", order.getId());
            return;
        }

        for (var item : order.getItems()) {
            log.info("order.stock_reduce.item orderId={} productId={} quantity={}",
                    order.getId(),
                    item.getProductId(), item.getQuantity());
            catalogClient.reduceStock(item.getProductId(), item.getQuantity());
        }
        log.info("order.stock_reduce.success orderId={}", order.getId());
    }

    /*
     * Helper method:
     * Get raw order entity by id for internal service use.
     */
    @Override
    public Order getOrderById(Long orderId) {
        log.info("order.internal.fetch_db orderId={}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    /*
     * What:
     * Fetches all orders for admin internal use.
     *
     * Why:
     * Adminservice needs complete order data for dashboard and order listing.
     *
     * How:
     * Query all orders in latest-first order and map each entity to OrderAdminDto.
     */
    public List<OrderAdminDto> getAllOrdersForAdmin() {
        log.info("order.admin.fetch_all.start");
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToOrderAdminDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    /*
     * What:
     * Fetches orders inside a date range for admin reports.
     *
     * Why:
     * Reports APIs require time-bounded data (sales/products/customers).
     *
     * How:
     * 1) Convert LocalDate range to full-day LocalDateTime range.
     * 2) Query repository by createdAt range.
     * 3) Map results to OrderAdminDto list.
     */
    public List<OrderAdminDto> getOrdersByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay().minusNanos(1);
        log.info("order.admin.fetch_range.start start={} end={}", start, end);
        return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(this::mapToOrderAdminDto)
                .toList();
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
        log.info("order.payment.start orderId={} userId={}", orderId, userId);

        // Step 1: Fetch order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        // Step 2: Authorization check
        if (!order.getUserId().equals(userId)) {
            log.warn("order.payment.unauthorized orderId={} requestedBy={} owner={}",
                    orderId, userId, order.getUserId());
            throw new UnauthorizedException("You are not allowed to initiate payment for this order");
        }

        // Step 3: Payment can start only from CHECKOUT.
        if (order.getStatus() != OrderStatus.CHECKOUT) {
            log.warn("order.payment.blocked orderId={} currentStatus={}", orderId, order.getStatus());
            throw new InvalidOrderStateException("Order is not in CHECKOUT state");
        }

        // Step 4: Call Payment Service
        PaymentResponseDto response = paymentClient.createPayment(buildPaymentRequest(order));
        log.info("order.payment.request_sent orderId={}", orderId);
        return response;
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

    /*
     * What:
     * Maps Order entity into admin reporting DTO.
     *
     * Why:
     * Adminservice should receive only required fields in a stable transport shape.
     *
     * How:
     * Map order-level fields and convert order items to OrderAdminItemDto list.
     */
    private OrderAdminDto mapToOrderAdminDto(Order order) {
        List<OrderAdminItemDto> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(item -> OrderAdminItemDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();

        return OrderAdminDto.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userName(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getFullName() : null)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .placedAt(order.getCreatedAt())
                .items(items)
                .build();
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
                .packedAt(order.getPackedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .address(addressDto)
                .items(items)
                .build();
    }
}
