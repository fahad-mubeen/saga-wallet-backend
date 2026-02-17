package com.example.sagawallet.saga;

public interface ISagaStep {

    Boolean execute(SagaContext context);

    Boolean compensate(SagaContext context);

    String getName();
}
