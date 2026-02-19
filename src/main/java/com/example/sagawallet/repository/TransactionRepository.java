package com.example.sagawallet.repository;

import com.example.sagawallet.entity.Transaction;
import com.example.sagawallet.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // all debit transactions for a given wallet
    List<Transaction> findBySourceWalletId(Long sourceWalletId);

    // all credit transactions for a given wallet
    List<Transaction> findByDestinationWalletId(Long destinationWalletId);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWalletId = :walletId OR t.destinationWalletId = :walletId")
    List<Transaction> findByWalletId(@Param("walletId") Long walletId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findBySagaInstanceId(Long sagaInstanceId);
}
