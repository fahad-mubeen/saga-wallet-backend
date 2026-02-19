package com.example.sagawallet.saga;

import com.example.sagawallet.entity.SagaInstance;

public interface ISagaOrchestrator {

    Long startSaga(SagaContext context);

    Boolean executeStep(Long sagaId, String stepName) throws Exception;

    Boolean compensateStep(Long sagaId, String stepName) throws Exception;

    SagaInstance getSagaInstance(Long sagaInstanceId);

    void completeSaga(Long sagaInstanceId);

    void compensateSaga(Long sagaInstanceId);

    void failSaga(Long sagaInstanceId);
}
