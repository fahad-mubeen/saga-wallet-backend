package com.example.sagawallet.saga;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
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
        return value instanceof BigDecimal ? (BigDecimal) value : null;
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value instanceof String ? value.toString() : null;
    }
}
