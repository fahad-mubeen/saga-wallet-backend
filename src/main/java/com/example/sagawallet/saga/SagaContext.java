package com.example.sagawallet.saga;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@Slf4j
public class SagaContext {
    private Map<String, Object> data;

    public SagaContext(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Long getLong(String key) {
        Object value = data.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = data.get(key);
        log.info("Getting BigDecimal for key: {}, value: {}", key, value);
        return value instanceof Number ? new BigDecimal(value.toString()) : null;
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value instanceof String ? value.toString() : null;
    }
}
