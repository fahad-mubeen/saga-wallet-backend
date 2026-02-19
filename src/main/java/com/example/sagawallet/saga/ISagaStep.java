package com.example.sagawallet.saga;

public interface ISagaStep {

    Boolean execute(SagaContext context) throws Exception;

    Boolean compensate(SagaContext context) throws Exception;

    String getName();
}
