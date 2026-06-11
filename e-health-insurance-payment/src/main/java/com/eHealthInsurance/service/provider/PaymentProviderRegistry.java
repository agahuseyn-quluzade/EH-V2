package com.eHealthInsurance.service.provider;

import com.eHealthInsurance.entity.enums.PaymentProvider;
import com.eHealthInsurance.exception.PaymentErrorEnum;
import com.eHealthInsurance.exception.PaymentException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentProviderRegistry {

    private final List<PaymentProviderClient> clients;

    public PaymentProviderRegistry(List<PaymentProviderClient> clients) {
        this.clients = List.copyOf(clients);
    }

    public PaymentProviderClient get(PaymentProvider provider) {
        return clients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new PaymentException(PaymentErrorEnum.PAYMENT_PROVIDER_NOT_SUPPORTED));
    }
}
