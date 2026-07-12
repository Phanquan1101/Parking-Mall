package com.parkflow.mall.merchant.repository;
import com.parkflow.mall.merchant.model.InvoiceValidation; import java.util.*;
public interface InvoiceValidationRepository { boolean existsByInvoiceCode(String invoiceCode); void save(InvoiceValidation validation); List<InvoiceValidation> findBySessionId(String sessionId); }
