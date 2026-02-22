package com.example.sagawallet.controller;

import com.example.sagawallet.dto.CreateWalletRequestDTO;
import com.example.sagawallet.dto.DebitWalletRequestDTO;
import com.example.sagawallet.entity.Wallet;
import com.example.sagawallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {
    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestBody CreateWalletRequestDTO request) {
        Wallet wallet = walletService.createWallet(request.getUserId());
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/balance/{id}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        BigDecimal balance = walletService.getBalance(id);
        return ResponseEntity.ok(balance);
    }

    @PutMapping("/debit/{id}")
    public ResponseEntity<Wallet> debit(@PathVariable Long id, @RequestBody DebitWalletRequestDTO request) {
        walletService.debit(id, request.getAmount());
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @PutMapping("/credit/{id}")
    public ResponseEntity<Wallet> credit(@PathVariable Long id, @RequestBody DebitWalletRequestDTO request) {
        walletService.credit(id, request.getAmount());
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }
}
