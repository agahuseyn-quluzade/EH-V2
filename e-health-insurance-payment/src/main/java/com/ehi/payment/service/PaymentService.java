package com.ehi.payment.service;

import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.PaymentReferenceType;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentDto processPayment(UUID userId, UUID referenceId, PaymentReferenceType referenceType, BigDecimal amount);

    List<PaymentDto> getMyPayments(UUID userId);

    PaymentDto getPaymentById(UUID paymentId, UUID requesterId, boolean privileged);

    PagedResponse<PaymentDto> getAllPayments(Pageable pageable);
}
