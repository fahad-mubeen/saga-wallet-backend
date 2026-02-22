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
public class DebitSourceWalletStep implements ISagaStep {
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public Boolean execute(SagaContext context) throws Exception {
        Long sourceWalletId = context.getLong("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Debiting source wallet {} with amount {}", sourceWalletId, amount);

        Wallet sourceWallet = walletRepository.findByIdWithLock(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Source wallet not found"));

        log.info("Source wallet found: {} and balance is {}", sourceWallet.getId(), sourceWallet.getBalance());
        context.put("sourceBalanceBeforeDebit", sourceWallet.getBalance());

        sourceWallet.debit(amount);
        walletRepository.save(sourceWallet);
        context.put("sourceWalletBalanceAfterDebit", sourceWallet.getBalance());
        log.info("Source wallet debited successfully");

        return true;
    }

    @Override
    @Transactional
    public Boolean compensate(SagaContext context) throws Exception {
        Long sourceWalletId = context.getLong("sourceWalletId");
        BigDecimal amount = context.getBigDecimal("amount");

        log.info("Compensating debit of source wallet {} with amount {}", sourceWalletId, amount);

        Wallet sourceWallet = walletRepository.findByIdWithLock(sourceWalletId)
                .orElseThrow(() -> new RuntimeException("Source wallet not found"));
        context.put("sourceBalanceBeforeDebitCompensation", sourceWallet.getBalance());
        log.info("Source wallet found: {} and balance is {}", sourceWallet.getId(), sourceWallet.getBalance());

        sourceWallet.credit(amount);
        walletRepository.save(sourceWallet);
        context.put("sourceWalletBalanceAfterDebitCompensation", sourceWallet.getBalance());
        log.info("Source wallet debit compensated successfully");

        return true;
    }

    @Override
    public String getName() {
        return SagaStepFactory.SagaStepType.DEBIT_SOURCE_WALLET_STEP.toString();
    }
}
