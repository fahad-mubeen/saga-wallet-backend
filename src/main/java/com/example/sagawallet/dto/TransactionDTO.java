package com.example.sagawallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private Long id;
    private Long sourceWalletId;
    private Long destinationWalletId;
    private String type; // "DEPOSIT", "WITHDRAWAL"
    private BigDecimal amount;
    private String status; // "PENDING", "COMPLETED", "FAILED"
    private String description;
    private Long sagaInstanceId;
}
