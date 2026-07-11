package com.nwhite.customerservice.service;

import com.nwhite.customerservice.dto.CustomerCreatedResponse;
import com.nwhite.customerservice.dto.CustomerRequest;
import com.nwhite.customerservice.entity.Customer;
import com.nwhite.customerservice.entity.KycStatus;
import com.nwhite.customerservice.mapper.CustomerMapper;
import com.nwhite.customerservice.repository.CustomerRepository;
import com.nwhite.customerservice.util.FingerPrint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerCreatedResponse create(CustomerRequest request) {
        String externalId = request.getExternalId();
        String fp = FingerPrint.customerCreate(request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress());

        // Fast path: same externalId already present?
        Optional<Customer> byExt = repository.findByExternalId(externalId);
        if (byExt.isPresent()) {
            Customer existing = byExt.get();
            if (fp.equals(existing.getRequestFingerprint())) {
                return mapper.toCreateResponse(existing); // idempotent replay
            }
            System.out.println("Conflict discovered! This External ID already exists!");
        }

        Customer entity = mapper.toEntity(request);
        entity.setActive(false);
        entity.setKycStatus(KycStatus.PENDING);
        entity.setRequestFingerprint(fp);
        entity.setCreatedAt(LocalDateTime.now());
        Customer saved = repository.saveAndFlush(entity);
        return mapper.toCreateResponse(saved);
    }
}
