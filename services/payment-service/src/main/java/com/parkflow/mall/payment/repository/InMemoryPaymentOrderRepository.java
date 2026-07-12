package com.parkflow.mall.payment.repository;
import com.parkflow.mall.payment.model.PaymentOrder;
import java.util.Map; import java.util.Optional; import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
@Repository public class InMemoryPaymentOrderRepository implements PaymentOrderRepository {
 private final Map<String,PaymentOrder> orders=new ConcurrentHashMap<>(); private final Map<String,String> creationKeys=new ConcurrentHashMap<>();
 public PaymentOrder save(PaymentOrder o){orders.put(o.id(),o);if(o.creationIdempotencyKey()!=null)creationKeys.putIfAbsent(o.creationIdempotencyKey(),o.id());return o;}
 public Optional<PaymentOrder> findById(String id){return Optional.ofNullable(orders.get(id));}
 public Optional<PaymentOrder> findByCreationKey(String key){return Optional.ofNullable(creationKeys.get(key)).flatMap(this::findById);}
}
