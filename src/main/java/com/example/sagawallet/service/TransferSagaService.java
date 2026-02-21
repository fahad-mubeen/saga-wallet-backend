package com.example.sagawallet.service;

import com.example.sagawallet.entity.Transaction;
import com.example.sagawallet.saga.ISagaOrchestrator;
import com.example.sagawallet.saga.SagaContext;
import com.example.sagawallet.saga.steps.SagaStepFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSagaService {
    private final TransactionService transactionService;
    private final ISagaOrchestrator sagaOrchestrator;

    @Transactional
    public Long initiateTransfer(
            Long sourceWalletId,
            Long destinationWalletId,
            BigDecimal amount,
            String description
    ) {
        log.info("Initiating transfer from wallet {} to wallet {} with amount {}", sourceWalletId, destinationWalletId, amount);
        Transaction transaction = transactionService.createTransaction(sourceWalletId, destinationWalletId, amount, description);

        SagaContext sagaContext = SagaContext.builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("sourceWalletId", sourceWalletId),
                        Map.entry("destinationWalletId", destinationWalletId),
                        Map.entry("amount", amount),
                        Map.entry("description", description)
                ))
                .build();

        Long sagaInstanceId = sagaOrchestrator.startSaga(sagaContext);
        log.info("Transfer saga initiated with saga instance id: {}", sagaInstanceId);
        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

        executeTransferSaga(sagaInstanceId);

        return sagaInstanceId;
    }

    @Transactional
    public void executeTransferSaga(Long sagaInstanceId) {
        log.info("Executing transfer saga with saga instance id: {}", sagaInstanceId);

        try {
            for (SagaStepFactory.SagaStepType stepType : SagaStepFactory.TransferMoneySagaSteps) {
                Boolean success = sagaOrchestrator.executeStep(sagaInstanceId, stepType.name());
                if (!success) {
                    log.error("Step {} failed for saga instance id: {}", stepType.name(), sagaInstanceId);
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return;
                }
            }
            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga completed successfully for saga instance id: {}", sagaInstanceId);
        } catch (Exception e) {
            log.info("Error occurred while executing transfer saga with saga instance id: {}", sagaInstanceId, e);
            sagaOrchestrator.failSaga(sagaInstanceId);
        }
    }
}
