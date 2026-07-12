package com.nwhite.customerservice.controller;

import com.nwhite.customerservice.dto.CustomerCreatedResponse;
import com.nwhite.customerservice.dto.CustomerRequest;
import com.nwhite.customerservice.dto.UpdateKycStatusRequest;
import com.nwhite.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService service;

    @PostMapping("/customers")
    public ResponseEntity<CustomerCreatedResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerCreatedResponse body = service.create(request);
        URI loc = URI.create("/api/v1/customers/" + body.getExternalId());
        return ResponseEntity.created(loc).body(body);
    }

    @PatchMapping("/customers/{id}/kyc-status")
    public ResponseEntity<Void> updateKycStatus(
            @PathVariable String id,
            @RequestBody UpdateKycStatusRequest request
    ) {
        Integer newVersion = service.updateKycStatus(id, request.getKycStatus());

        // Return 204 No Content, only ETag
        return ResponseEntity.noContent()
                .eTag("\\" + newVersion + "\\")
                .build();
    }
}
