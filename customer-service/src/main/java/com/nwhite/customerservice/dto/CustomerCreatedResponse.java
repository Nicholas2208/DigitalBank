package com.nwhite.customerservice.dto;

import com.nwhite.customerservice.entity.KycStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreatedResponse {
    private String externalId;
    private KycStatus kycStatus;
    private Integer version;
    private LocalDateTime createdAt;
}
