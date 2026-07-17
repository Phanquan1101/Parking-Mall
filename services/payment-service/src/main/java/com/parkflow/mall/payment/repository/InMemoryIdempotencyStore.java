package com.parkflow.mall.payment.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryIdempotencyStore {

    private final Map<String, String> values = new ConcurrentHashMap<>();

    public String get(String key) {
        return values.get(key);
    }

    public String putIfAbsent(String key, String value) {
        return values.putIfAbsent(key, value);
    }
}