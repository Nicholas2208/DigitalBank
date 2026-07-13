package com.nwhite.customerservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nwhite.customerservice.entity.KycStatus;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCustomerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String externalId;
    private KycStatus kycStatus;
    private String requestFingerprint;
    private Boolean active;
}
