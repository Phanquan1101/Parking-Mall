package com.parkflow.mall.payment.repository;

import com.parkflow.mall.payment.model.PaymentOrder;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentOrderRepository implements PaymentOrderRepository {

 private final Map<String, PaymentOrder> orders = new ConcurrentHashMap<>();
 private final Map<String, String> creationKeys = new ConcurrentHashMap<>();

 public PaymentOrder save(PaymentOrder order) {
  orders.put(order.id(), order);

  if (order.creationIdempotencyKey() != null) {
   creationKeys.putIfAbsent(order.creationIdempotencyKey(), order.id());
  }

  return order;
 }

 public Optional<PaymentOrder> findById(String id) {
  return Optional.ofNullable(orders.get(id));
 }

 public Optional<PaymentOrder> findByCreationKey(String key) {
  return Optional.ofNullable(creationKeys.get(key))
          .flatMap(this::findById);
 }

 public java.util.List<PaymentOrder> findAll() {
  return java.util.List.copyOf(orders.values());
 }
}