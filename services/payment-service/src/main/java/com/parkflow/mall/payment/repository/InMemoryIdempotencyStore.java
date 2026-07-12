package com.parkflow.mall.payment.repository;
import java.util.Map; import java.util.concurrent.ConcurrentHashMap; import org.springframework.stereotype.Repository;
@Repository public class InMemoryIdempotencyStore { private final Map<String,String> values=new ConcurrentHashMap<>(); public String get(String key){return values.get(key);} public String putIfAbsent(String key,String value){return values.putIfAbsent(key,value);} }
