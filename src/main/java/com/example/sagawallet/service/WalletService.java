package com.example.sagawallet.service;

import com.example.sagawallet.entity.Wallet;
import com.example.sagawallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional
    public Wallet createWallet(Long userId) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Wallet created successfully with id: {}", wallet.getId());
        return wallet;
    }

    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Transactional
    public void debit(Long walletId, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);
        wallet.debit(amount);
        walletRepository.save(wallet);
    }

    @Transactional
    public void credit(Long walletId, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);
        wallet.credit(amount);
        walletRepository.save(wallet);
    }

    public BigDecimal getBalance(Long walletId) {
        Wallet wallet = getWalletById(walletId);
        return wallet.getBalance();
    }
}
