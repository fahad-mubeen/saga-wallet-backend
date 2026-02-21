package com.example.sagawallet.entity;

import com.example.sagawallet.enums.TransactionStatus;
import com.example.sagawallet.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "source_wallet_id", nullable = false)
    private Long sourceWalletId;

    @Column(name = "destination_wallet_id", nullable = false)
    private Long destinationWalletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type = TransactionType.TRANSFER;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "saga_instance_id", nullable = false)
    private Long sagaInstanceId;
}
