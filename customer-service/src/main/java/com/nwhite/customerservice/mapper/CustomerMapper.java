package com.nwhite.customerservice.mapper;

import com.nwhite.customerservice.dto.CustomerCreatedResponse;
import com.nwhite.customerservice.dto.CustomerRequest;
import com.nwhite.customerservice.entity.Customer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        return Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .externalId(request.getExternalId())
                .build();
    }

    public CustomerCreatedResponse toCreateResponse(Customer customer) {
        return CustomerCreatedResponse.builder()
                .externalId(customer.getExternalId())
                .kycStatus(customer.getKycStatus())
                .version(customer.getVersion())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
