package com.ehi.payment.service;

import java.util.UUID;

public interface PaymentProcessor {

    void process(UUID paymentId);
}
