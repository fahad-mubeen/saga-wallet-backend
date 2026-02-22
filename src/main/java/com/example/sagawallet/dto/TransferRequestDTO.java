package com.example.sagawallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    private Long sourceWalletId;
    private Long destinationWalletId;
    private BigDecimal amount;
    private String description;
}
