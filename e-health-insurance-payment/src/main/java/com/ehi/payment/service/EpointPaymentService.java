package com.ehi.payment.service;

import com.ehi.payment.dto.request.EpointInitPaymentRequest;
import com.ehi.payment.dto.request.EpointReverseRequest;
import com.ehi.payment.dto.response.EpointPaymentResponse;

import java.util.UUID;

public interface EpointPaymentService {

    EpointPaymentResponse initPayment(UUID userId, EpointInitPaymentRequest request);

    EpointPaymentResponse processCallback(String data, String signature);

    EpointPaymentResponse getStatus(UUID transactionId, UUID requesterId, boolean privileged);

    EpointPaymentResponse syncStatus(UUID transactionId, UUID requesterId, boolean privileged);

    EpointPaymentResponse reverse(UUID transactionId, UUID requesterId, boolean privileged, EpointReverseRequest request);
}
