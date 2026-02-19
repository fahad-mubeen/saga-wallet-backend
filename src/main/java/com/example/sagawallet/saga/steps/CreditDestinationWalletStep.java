package com.example.sagawallet.saga.steps;

import com.example.sagawallet.entity.Wallet;
import com.example.sagawallet.repository.WalletRepository;
import com.example.sagawallet.saga.ISagaStep;
import com.example.sagawallet.saga.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditDestinationWalletStep implements ISagaStep {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public Boolean execute(SagaContext context) throws Exception {
        // step 1: get destination wallet id from context
        Long destinationWalletId = context.getLong("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Crediting destination wallet {} with amount {}", destinationWalletId, amount);

        // step 2: fetch the destination wallet from the database with a lock
        Wallet destinationWallet = walletRepository.findByIdWithLock(destinationWalletId)
                .orElseThrow(() -> new RuntimeException("Destination wallet not found"));

        log.info("Destination wallet found: {} and balance is {}", destinationWallet.getId(), destinationWallet.getBalance());

        context.put("destinationBalanceBeforeCredit", destinationWallet.getBalance());

        // step 3: credit the amount to the destination wallet
        destinationWallet.credit(amount);
        walletRepository.save(destinationWallet);

        // step 4: update the context with the changes
        context.put("destinationWalletBalanceAfterCredit", destinationWallet.getBalance());

        // note:- if an error occurs after crediting the destination wallet and saving into context,
        // the compensate method will use the balance before crediting to revert the changes.
        // consider DB as the surce of truth over in memory context data
        log.info("Destination wallet credited successfully");
        return true;
    }

    @Override
    @Transactional
    public Boolean compensate(SagaContext context) {
        // step 1: get destination wallet id from context
        Long destinationWalletId = context.getLong("destinationWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Compensating credit of destination wallet {} with amount {}", destinationWalletId, amount);

        // step 2: fetch the destination wallet from the database with a lock
        Wallet destinationWallet = walletRepository.findByIdWithLock(destinationWalletId)
                .orElseThrow(() -> new RuntimeException("Destination wallet not found"));

        log.info("Destination wallet found: {} and balance is {}", destinationWallet.getId(), destinationWallet.getBalance());

        context.put("destinationBalanceBeforeCreditCompensation", destinationWallet.getBalance());

        // step 3: debit the amount to the destination wallet
        destinationWallet.debit(amount);
        walletRepository.save(destinationWallet);

        // step 4: update the context with the changes
        context.put("destinationWalletBalanceAfterCreditCompensation", destinationWallet.getBalance());

        log.info("Destination wallet compensation completed successfully");
        return true;
    }

    @Override
    public String getName() {
        return "CreditDestinationWalletStep";
    }
}
