package com.example.sagawallet.mapper;

import com.example.sagawallet.dto.TransactionDTO;
import com.example.sagawallet.entity.Transaction;
import com.example.sagawallet.enums.TransactionStatus;
import com.example.sagawallet.enums.TransactionType;

public class TransactionMapper {
    public static Transaction toEntity(TransactionDTO dto) {
        return Transaction.builder()
                .amount(dto.getAmount())
                .sourceWalletId(dto.getSourceWalletId())
                .destinationWalletId(dto.getDestinationWalletId())
                .description(dto.getDescription())
                .sagaInstanceId(dto.getSagaInstanceId())
                .build();
    }
}
