package com.eHealthInsurance.service.provider;

import com.eHealthInsurance.entity.Payment;
import com.eHealthInsurance.entity.Refund;
import com.eHealthInsurance.entity.enums.PaymentProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProviderClient implements PaymentProviderClient {

    @Override
    public boolean supports(PaymentProvider provider) {
        return provider == PaymentProvider.MOCK
                || provider == PaymentProvider.MANUAL
                || provider == PaymentProvider.BANK_TRANSFER
                || provider == PaymentProvider.STRIPE;
    }

    @Override
    public ProviderPaymentResult initiate(Payment payment, String paymentMethodToken) {
        return new ProviderPaymentResult(reference("init"));
    }

    @Override
    public ProviderPaymentResult confirm(Payment payment, String providerReference) {
        return new ProviderPaymentResult(hasText(providerReference) ? providerReference : reference("success"));
    }

    @Override
    public ProviderFailureResult fail(
            Payment payment,
            String providerReference,
            String failureCode,
            String failureReason
    ) {
        return new ProviderFailureResult(
                hasText(providerReference) ? providerReference : reference("failed"),
                hasText(failureCode) ? failureCode : "MOCK_FAILURE",
                hasText(failureReason) ? failureReason : "Mock provider failure"
        );
    }

    @Override
    public ProviderRefundResult refund(Payment payment, Refund refund) {
        return new ProviderRefundResult(reference("refund"));
    }

    private String reference(String prefix) {
        return "mock_" + prefix + "_" + UUID.randomUUID();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
