package com.parkflow.mall.payment.repository;

import com.parkflow.mall.payment.model.PaymentOrder;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository {

    PaymentOrder save(PaymentOrder order);

    Optional<PaymentOrder> findById(String id);

    Optional<PaymentOrder> findByCreationKey(String key);

    List<PaymentOrder> findAll();
}