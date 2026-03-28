package com.shopsphere.orderservice.service;

import com.shopsphere.orderservice.dto.CheckoutRequestDto;
import com.shopsphere.orderservice.dto.CheckoutResponseDto;

public interface CheckoutService {

    CheckoutResponseDto startCheckout(Long userId, CheckoutRequestDto request);
}