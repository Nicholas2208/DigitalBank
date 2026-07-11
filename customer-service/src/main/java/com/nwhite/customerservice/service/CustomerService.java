package com.nwhite.customerservice.service;

import com.nwhite.customerservice.dto.CustomerCreatedResponse;
import com.nwhite.customerservice.dto.CustomerRequest;
import com.nwhite.customerservice.entity.Customer;
import com.nwhite.customerservice.entity.KycStatus;
import com.nwhite.customerservice.mapper.CustomerMapper;
import com.nwhite.customerservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerCreatedResponse create(CustomerRequest request) {
        Customer entity = mapper.toEntity(request);
        entity.setActive(false);
        entity.setKycStatus(KycStatus.PENDING);
        entity.setRequestFingerprint("abc");
        entity.setCreatedAt(LocalDateTime.now());
        Customer saved = repository.saveAndFlush(entity);
        return mapper.toCreateResponse(saved);
    }
}
