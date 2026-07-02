package com.example.sagawallet.saga;

import com.example.sagawallet.entity.SagaInstance;
import com.example.sagawallet.entity.SagaStep;
import com.example.sagawallet.enums.SagaStatus;
import com.example.sagawallet.enums.SagaStepStatus;
import com.example.sagawallet.repository.SagaInstanceRepository;
import com.example.sagawallet.repository.SagaStepRepository;
import com.example.sagawallet.exception.SagaInstanceNotFoundException;
import com.example.sagawallet.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator implements ISagaOrchestrator {

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
        sagaStep.setStatus(SagaStepStatus.RUNNING);
        sagaStepRepository.save(sagaStep);
        Boolean isStepExecutedSuccessfully = iSagaStep.execute(sagaContext);
        if(isStepExecutedSuccessfully) {
            sagaStep.setStatus(SagaStepStatus.COMPLETED);
            sagaStepRepository.save(sagaStep);

            sagaInstance.setCurrentStep(stepName);
            sagaInstance.setStatus(SagaStatus.RUNNING);
            sagaInstanceRepository.save(sagaInstance);

            log.info("Step: {} executed successfully for saga instance: {}", stepName, sagaId);
            return true;
        }

        sagaStep.setStatus(SagaStepStatus.FAILED);
        sagaStepRepository.save(sagaStep);
        log.error("Step: {} execution failed for saga instance: {}", stepName, sagaId);
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

        if (sagaStep.getId() == null) {
            log.warn("Step not found with name: {} for saga instance: {}. It might be a new step or the step execution might have failed in the previous attempt. Skipping compensation for this step.", stepName, sagaId);
            return true;
        }

        SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);
        sagaStep.setStatus(SagaStepStatus.COMPENSATING);
        sagaStepRepository.save(sagaStep);
        Boolean isStepExecutedSuccessfully = iSagaStep.compensate(sagaContext);
        if(isStepExecutedSuccessfully) {
            sagaStep.setStatus(SagaStepStatus.COMPENSATED);
            sagaStepRepository.save(sagaStep);

            log.info("Step: {} compensated successfully for saga instance: {}", stepName, sagaId);
            return true;
        }

        sagaStep.setStatus(SagaStepStatus.FAILED);
        sagaStepRepository.save(sagaStep);
        log.error("Step: {} execution failed for saga instance: {}", stepName, sagaId);
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
// 1. Add retry mechanism for failed steps with exponential backoff strategy.
// 2. Add support for parallel steps in the saga.
// 3. Add monitoring and alerting for failed saga instances and steps.
// 4. Instead of manually setting the status of saga steps and saga instance, we can introduce methods within entities