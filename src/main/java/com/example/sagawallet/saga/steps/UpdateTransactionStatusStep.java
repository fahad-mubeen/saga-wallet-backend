package com.example.sagawallet.saga.steps;

import com.example.sagawallet.entity.Transaction;
import com.example.sagawallet.enums.TransactionStatus;
import com.example.sagawallet.exception.TransactionNotFoundException;
import com.example.sagawallet.repository.TransactionRepository;
import com.example.sagawallet.saga.ISagaStep;
import com.example.sagawallet.saga.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTransactionStatusStep implements ISagaStep {
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Boolean execute(SagaContext context) throws Exception {
        Long transactionId = context.getLong("transactionId");

        log.info("Updating transaction status for transaction id {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));

        context.put("originalTransactionStatus", transaction.getStatus());

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        context.put("updatedTransactionStatus", transaction.getStatus());
        log.info("Transaction status updated successfully for transaction id {}", transactionId);
        return true;
    }

    @Override
    @Transactional
    public Boolean compensate(SagaContext context) throws Exception {
        Long transactionId = context.getLong("transactionId");

        TransactionStatus originalStatus = TransactionStatus.valueOf(context.getString("originalTransactionStatus"));

        log.info("Compensating transaction status update for transaction id {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));

        transaction.setStatus(originalStatus);
        transactionRepository.save(transaction);
        log.info("Transaction status reverted successfully for transaction id {}", transactionId);
        return true;
    }

    @Override
    public String getName() {
        return SagaStepFactory.SagaStepType.UPDATE_TRANSACTION_STATUS_STEP.toString();
    }
}
