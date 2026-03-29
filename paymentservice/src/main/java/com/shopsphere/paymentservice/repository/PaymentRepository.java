package com.shopsphere.paymentservice.repository;


import com.shopsphere.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByIdempotencyKeyOrderByCreatedAtDescIdDesc(String key);

    List<Payment> findAllByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}