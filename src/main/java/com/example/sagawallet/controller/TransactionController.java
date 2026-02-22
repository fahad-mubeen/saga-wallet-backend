package com.example.sagawallet.controller;

import com.example.sagawallet.dto.TransferRequestDTO;
import com.example.sagawallet.dto.TransferResponseDTO;
import com.example.sagawallet.service.TransactionService;
import com.example.sagawallet.service.TransferSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final TransferSagaService transferSagaService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDTO> initiateTransferTransaction(@RequestBody TransferRequestDTO requestDTO) {
        Long sagaInstanceID = transferSagaService.initiateTransfer(
          requestDTO.getSourceWalletId(),
          requestDTO.getDestinationWalletId(),
          requestDTO.getAmount(),
          requestDTO.getDescription()
        );

        TransferResponseDTO responseDTO = TransferResponseDTO.builder()
                .sagaInstanceId(sagaInstanceID)
                .build();
        return ResponseEntity.ok(responseDTO);
    }

}
