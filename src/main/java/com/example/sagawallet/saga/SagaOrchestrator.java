package com.example.sagawallet.saga;

import com.example.sagawallet.entity.SagaInstance;
import com.example.sagawallet.entity.SagaStep;
import com.example.sagawallet.enums.SagaStatus;
import com.example.sagawallet.enums.SagaStepStatus;
import com.example.sagawallet.repository.SagaInstanceRepository;
import com.example.sagawallet.repository.SagaStepRepository;
import com.example.sagawallet.exception.SagaInstanceNotFoundException;
import com.example.sagawallet.exception.WalletNotFoundException;
import com.example.sagawallet.exception.InsufficientFundsException;
import com.example.sagawallet.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator implements ISagaOrchestrator {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_INTERVAL_MS = 100;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepFactory sagaStepFactory;
    private final ObjectMapper objectMapper;
    private final SagaStepRepository sagaStepRepository;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {
        try {
            // convert the context to json string
            String contextJson = objectMapper.writeValueAsString(context);
            SagaInstance sagaInstance = SagaInstance.builder()
                    .context(contextJson)
                    .status(SagaStatus.STARTED)
                    .build();
            SagaInstance savedSagaInstance = sagaInstanceRepository.save(sagaInstance);
            log.info("Saga instance created with id: {}", savedSagaInstance.getId());
            return savedSagaInstance.getId();
        } catch (Exception e) {
            log.error("Error while creating saga instance", e);
            throw new IllegalArgumentException("Error while creating saga instance", e);
        }
    }

    @Override
    @Transactional
    public Boolean executeStep(Long sagaId, String stepName) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new SagaInstanceNotFoundException("Saga instance not found with id: " + sagaId));

        ISagaStep iSagaStep = sagaStepFactory.getSagaStep(stepName)
                .orElseThrow(() -> new IllegalArgumentException("Saga step not found with name: " + stepName));

        log.info("Executing step: {} for saga instance: {}", stepName, sagaId);

        SagaStep sagaStep = sagaStepRepository.findBySagaInstanceIdAndStepNameAndStatus(sagaId, stepName, SagaStepStatus.PENDING)
                .orElse(SagaStep.builder()
                .sagaInstanceId(sagaInstance.getId())
                .stepName(stepName)
                .status(SagaStepStatus.PENDING)
                .build());

        if (sagaStep.getId() == null) {
            sagaStep = sagaStepRepository.save(sagaStep);
            log.info("Saga step: {} is being executed for the first time for saga instance: {}", stepName, sagaId);
        } else {
            log.info("Saga step: {} is being retried for saga instance: {}", stepName, sagaId);
        }

        SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            sagaStep.setStatus(SagaStepStatus.RUNNING);
            sagaStep = sagaStepRepository.save(sagaStep);

            try {
                Boolean isStepExecutedSuccessfully = iSagaStep.execute(sagaContext);
                if (isStepExecutedSuccessfully) {
                    sagaStep.setStatus(SagaStepStatus.COMPLETED);
                    sagaStepRepository.save(sagaStep);

                    sagaInstance.setCurrentStep(stepName);
                    sagaInstance.setStatus(SagaStatus.RUNNING);
                    sagaInstanceRepository.save(sagaInstance);

                    log.info("Step: {} executed successfully for saga instance: {}", stepName, sagaId);
                    return true;
                } else {
                    sagaStep.setStatus(SagaStepStatus.FAILED);
                    sagaStepRepository.save(sagaStep);
                    log.error("Step: {} execution failed (returned false) for saga instance: {}", stepName, sagaId);
                    return false;
                }
            } catch (InsufficientFundsException | WalletNotFoundException e) {
                // Terminal business errors mean the transaction is logically impossible; skip retries and trigger compensation
                log.error("Business error in step: {} for saga instance: {}. Error: {}", stepName, sagaId, e.getMessage());
                sagaStep.setStatus(SagaStepStatus.FAILED);
                sagaStep.setErrorMessage(e.getMessage());
                sagaStepRepository.save(sagaStep);
                return false;
            } catch (PessimisticLockingFailureException e) {
                // Intercepting transient shard locking timeouts here protects the master transaction context from being marked rollback-only
                log.warn("Database concurrency failure in step: {} for saga instance: {}. Attempt {} of {}. Error: {}", 
                         stepName, sagaId, attempt, MAX_ATTEMPTS, e.getMessage());

                if (attempt >= MAX_ATTEMPTS) {
                    log.error("Terminal database concurrency failure in step: {} for saga instance: {} after {} attempts.", 
                              stepName, sagaId, MAX_ATTEMPTS);
                    sagaStep.setStatus(SagaStepStatus.FAILED);
                    sagaStep.setErrorMessage("Exhausted retries: " + e.getMessage());
                    sagaStepRepository.save(sagaStep);
                    return false;
                }

                // Pause the executing worker thread to allow shard lock contentions to clear naturally before retrying
                long delay = (long) (INITIAL_INTERVAL_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                log.info("Sleeping for {} ms before retry attempt {} for step: {}", delay, attempt + 1, stepName);
                Thread.sleep(delay);
            }
        }
        return false;
    }

    @Override
    @Transactional
    public Boolean compensateStep(Long sagaId, String stepName) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new SagaInstanceNotFoundException("Saga instance not found with id: " + sagaId));

        ISagaStep iSagaStep = sagaStepFactory.getSagaStep(stepName)
                .orElseThrow(() -> new IllegalArgumentException("Saga step not found with name: " + stepName));

        log.info("Executing step: {} for saga instance: {}", stepName, sagaId);

        SagaStep sagaStep = sagaStepRepository.findBySagaInstanceIdAndStepNameAndStatus(sagaId, stepName, SagaStepStatus.COMPLETED)
                .orElse(null);

        if (sagaStep == null) {
            log.warn("Step not found with name: {} for saga instance: {}. It might be a new step or the step execution might have failed in the previous attempt. Skipping compensation for this step.", stepName, sagaId);
            return true;
        }

        SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            sagaStep.setStatus(SagaStepStatus.COMPENSATING);
            sagaStep = sagaStepRepository.save(sagaStep);

            try {
                Boolean isStepExecutedSuccessfully = iSagaStep.compensate(sagaContext);
                if (isStepExecutedSuccessfully) {
                    sagaStep.setStatus(SagaStepStatus.COMPENSATED);
                    sagaStepRepository.save(sagaStep);

                    log.info("Step: {} compensated successfully for saga instance: {}", stepName, sagaId);
                    return true;
                } else {
                    sagaStep.setStatus(SagaStepStatus.FAILED);
                    sagaStepRepository.save(sagaStep);
                    log.error("Step: {} compensation failed (returned false) for saga instance: {}", stepName, sagaId);
                    return false;
                }
            } catch (PessimisticLockingFailureException e) {
                // Intercepting transient shard locking timeouts here protects the master transaction context from being marked rollback-only
                log.warn("CRITICAL WARNING: Compensation step {} failed due to database concurrency conflict for saga instance {}. Attempt {} of {}. Error: {}", 
                         stepName, sagaId, attempt, MAX_ATTEMPTS, e.getMessage());

                if (attempt >= MAX_ATTEMPTS) {
                    log.error("CRITICAL: Saga compensation failed completely after retries. Manual intervention required for Saga ID: {}. Error: {}", 
                              sagaId, e.getMessage());
                    sagaStep.setStatus(SagaStepStatus.FAILED);
                    sagaStep.setErrorMessage("CRITICAL: Compensation failed: " + e.getMessage());
                    sagaStepRepository.save(sagaStep);
                    return false;
                }

                // Pause the executing worker thread to allow shard lock contentions to clear naturally before retrying
                long delay = (long) (INITIAL_INTERVAL_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                log.info("Sleeping for {} ms before retry attempt {} for compensation step: {}", delay, attempt + 1, stepName);
                Thread.sleep(delay);
            }
        }
        return false;
    }

    @Override
    @Transactional
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new SagaInstanceNotFoundException("Saga instance not found with id: " + sagaInstanceId));
    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new SagaInstanceNotFoundException("Saga instance not found with id: " + sagaInstanceId));
        sagaInstance.setStatus(SagaStatus.COMPLETED);
        sagaInstanceRepository.save(sagaInstance);
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new SagaInstanceNotFoundException("Saga instance not found with id: " + sagaInstanceId));

        sagaInstance.setStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(sagaInstance);

        List<SagaStep> completedSteps = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId);

        Boolean allStepsCompensatedSuccessfully = true;
        for (SagaStep sagaStep : completedSteps) {
            Boolean isCompensatedSuccessfully = compensateStep(sagaInstanceId, sagaStep.getStepName());
            if (!isCompensatedSuccessfully) {
                allStepsCompensatedSuccessfully = false;
            }
        }

        if (allStepsCompensatedSuccessfully) {
            sagaInstance.setStatus(SagaStatus.COMPENSATED);
            sagaInstanceRepository.save(sagaInstance);
            log.info("Saga instance with id: {} compensated successfully", sagaInstanceId);
        } else {
            log.error("Saga instance with id: {} compensation failed", sagaInstanceId);
        }
    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new SagaInstanceNotFoundException("Saga instance not found with id: " + sagaInstanceId));
        sagaInstance.setStatus(SagaStatus.FAILED);
        sagaInstanceRepository.save(sagaInstance);

        try {
            compensateSaga(sagaInstanceId);
        }
        catch (Exception e) {
            log.info("Error while compensating saga instance with id: {}. Marking saga instance as failed.", sagaInstanceId, e);
        }
    }
}


// Future improvements:
// 1. Add support for parallel steps in the saga.
// 2. Add monitoring and alerting for failed saga instances and steps.
// 3. Instead of manually setting the status of saga steps and saga instance, we can introduce methods within entities