package com.shopsphere.orderservice.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
}