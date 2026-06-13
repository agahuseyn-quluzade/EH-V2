package com.ehi.payment.service;

import com.ehi.payment.dto.response.CardRegistrationResponse;
import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.dto.response.SavedCardDto;

import java.util.List;
import java.util.UUID;

public interface EpointPaymentService {

    void handleCallback(String data, String signature);

    PaymentDto refreshStatus(UUID paymentId, UUID requesterId, boolean privileged);

    CardRegistrationResponse startCardRegistration(UUID userId);

    List<SavedCardDto> getMyCards(UUID userId);
}
