package com.shopsphere.paymentservice.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.paymentservice.enums.Gateway;
import com.shopsphere.paymentservice.enums.PaymentMethod;
import com.shopsphere.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long userId;


    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(unique = true)
    private String razorpayOrderId;

    @Enumerated(EnumType.STRING)
    private Gateway gateway;

    // Captures the user-selected payment method (COD/UPI/CARD).
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String currency;

    private Long amountInPaise;

    private String transactionId;

    private String failureReason;

    private String idempotencyKey;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Kolkata")
    private LocalDateTime createdAt;
}