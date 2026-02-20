package com.example.sagawallet.saga.steps;

import com.example.sagawallet.saga.ISagaStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SagaStepFactory {
    public static enum SagaStepType {
        DEBIT_SOURCE_WALLET_STEP,
        CREDIT_DESTINATION_WALLET_STEP,
        UPDATE_TRANSACTION_STATUS_STEP
    }

    private final Map<String, ISagaStep> sagaStepMap;

    public SagaStepFactory(List<ISagaStep> sagaSteps) {
        this.sagaStepMap = sagaSteps
                .stream().
                collect(Collectors.toMap(ISagaStep::getName, step -> step));
    }

    public Optional<ISagaStep> getSagaStep(String stepName) {
//        if(!sagaStepMap.containsKey(stepName)) {
//            throw new IllegalArgumentException("Saga step not found: " + stepName);
//        }
//        return sagaStepMap.get(stepName);
        return Optional.ofNullable(sagaStepMap.get(stepName));
    }
}
