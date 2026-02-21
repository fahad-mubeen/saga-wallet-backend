package com.example.sagawallet.service;

import com.example.sagawallet.dto.TransactionDTO;
import com.example.sagawallet.entity.Transaction;
import com.example.sagawallet.enums.TransactionStatus;
import com.example.sagawallet.mapper.TransactionMapper;
import com.example.sagawallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = TransactionMapper.toEntity(transactionDTO);
        transaction = transactionRepository.save(transaction);
        log.info("Transaction created successfully with id: {}", transaction.getId());
        return transaction;
    }

    @Transactional
    public Transaction createTransaction(Long sourceWalletId, Long destinationWalletId, BigDecimal amount, String description) {
        Transaction transaction = Transaction.builder()
                .sourceWalletId(sourceWalletId)
                .destinationWalletId(destinationWalletId)
                .amount(amount)
                .description(description)
                .status(TransactionStatus.PENDING)
                .build();
        transaction = transactionRepository.save(transaction);
        log.info("Transaction created successfully with id: {}", transaction.getId());
        return transaction;
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public List<Transaction> getTransactionsByWalletId(Long walletId) {
        return transactionRepository.findByWalletId(walletId);
    }

    public List<Transaction> getTransactionsBySourceWalletId(Long sourceWalletId) {
        return transactionRepository.findBySourceWalletId(sourceWalletId);
    }

    public List<Transaction> getTransactionsByDestinationWalletId(Long destinationWalletId) {
        return transactionRepository.findByDestinationWalletId(destinationWalletId);
    }

    public List<Transaction> getTransactionsBySagaInstanceId(Long sagaInstanceId) {
        return transactionRepository.findBySagaInstanceId(sagaInstanceId);
    }

    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public void updateTransactionWithSagaInstanceId(Long transactionId, Long sagaInstanceId) {
        Transaction transaction = getTransactionById(transactionId);
        transaction.setSagaInstanceId(sagaInstanceId);
        transactionRepository.save(transaction);
        log.info("Transaction with id: {} updated with saga instance id: {}", transactionId, sagaInstanceId);
    }
}
