package com.eHealthInsurance.service;

import com.eHealthInsurance.dto.request.ConfirmPaymentRequest;
import com.eHealthInsurance.dto.request.CreatePaymentRequest;
import com.eHealthInsurance.dto.request.CreateRefundRequest;
import com.eHealthInsurance.dto.request.FailPaymentRequest;
import com.eHealthInsurance.dto.response.InvoiceResponse;
import com.eHealthInsurance.dto.response.PaymentResponse;
import com.eHealthInsurance.dto.response.RefundResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPayment(UUID memberId, CreatePaymentRequest request);

    PaymentResponse createPayment(UUID memberId, CreatePaymentRequest request, String idempotencyKey);

    PaymentResponse getPayment(UUID paymentId, UUID requesterId, String role);

    List<PaymentResponse> getMemberPayments(UUID memberId);

    PaymentResponse confirmPayment(UUID paymentId, ConfirmPaymentRequest request);

    PaymentResponse failPayment(UUID paymentId, FailPaymentRequest request);

    List<InvoiceResponse> getInvoicesByPolicyId(UUID policyId);

    RefundResponse createRefund(UUID memberId, CreateRefundRequest request);

    List<RefundResponse> getMemberRefunds(UUID memberId);
}
