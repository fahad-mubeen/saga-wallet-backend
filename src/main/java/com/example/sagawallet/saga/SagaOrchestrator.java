package com.example.sagawallet.saga;

import com.example.sagawallet.entity.SagaInstance;
import com.example.sagawallet.entity.SagaStep;
import com.example.sagawallet.enums.SagaStatus;
import com.example.sagawallet.enums.SagaStepStatus;
import com.example.sagawallet.repository.SagaInstanceRepository;
import com.example.sagawallet.repository.SagaStepRepository;
import com.example.sagawallet.saga.steps.SagaStepFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator implements ISagaOrchestrator {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepFactory sagaStepFactory;
    private final ObjectMapper objectMapper;
    private final SagaStepRepository sagaStepRepository;

    @Override
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
            throw new RuntimeException("Error while creating saga instance", e);
        }
    }

    @Override
    public Boolean executeStep(Long sagaId, String stepName) throws Exception {
        SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found with id: " + sagaId));

        ISagaStep iSagaStep = sagaStepFactory.getSagaStep(stepName)
                .orElseThrow(() -> new RuntimeException("Saga step not found with name: " + stepName));

        log.info("Executing step: {} for saga instance: {}", stepName, sagaId);

        SagaStep sagaStep = SagaStep.builder()
                .sagaInstanceId(sagaInstance.getId())
                .stepName(stepName)
                .status(SagaStepStatus.PENDING)
                .build();
        sagaStep = sagaStepRepository.save(sagaStep);

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
    public Boolean compensateStep(Long sagaId, String stepName) throws Exception {
        return null;
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return null;
    }

    @Override
    public void completeSaga(Long sagaInstanceId) {

    }

    @Override
    public void compensateSaga(Long sagaInstanceId) {

    }

    @Override
    public void failSaga(Long sagaInstanceId) {

    }
}
