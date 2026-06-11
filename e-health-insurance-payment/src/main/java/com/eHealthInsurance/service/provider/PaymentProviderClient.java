package com.eHealthInsurance.service.provider;

import com.eHealthInsurance.entity.Payment;
import com.eHealthInsurance.entity.Refund;
import com.eHealthInsurance.entity.enums.PaymentProvider;

public interface PaymentProviderClient {

    boolean supports(PaymentProvider provider);

    ProviderPaymentResult initiate(Payment payment, String paymentMethodToken);

    ProviderPaymentResult confirm(Payment payment, String providerReference);

    ProviderFailureResult fail(Payment payment, String providerReference, String failureCode, String failureReason);

    ProviderRefundResult refund(Payment payment, Refund refund);
}
