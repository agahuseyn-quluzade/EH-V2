package com.eHealthInsurance.controller;

import com.eHealthInsurance.dto.request.ConfirmPaymentRequest;
import com.eHealthInsurance.dto.request.CreatePaymentRequest;
import com.eHealthInsurance.dto.request.CreateRefundRequest;
import com.eHealthInsurance.dto.request.FailPaymentRequest;
import com.eHealthInsurance.dto.response.InvoiceResponse;
import com.eHealthInsurance.dto.response.PaymentResponse;
import com.eHealthInsurance.dto.response.RefundResponse;
import com.eHealthInsurance.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        UUID memberId = UUID.fromString(authentication.getName());
        PaymentResponse response = paymentService.createPayment(memberId, request, idempotencyKey);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/payments/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        UUID memberId = UUID.fromString(authentication.getName());
        PaymentResponse response = paymentService.createPayment(memberId, request, idempotencyKey);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/payments/me")
    public ResponseEntity<List<PaymentResponse>> getMemberPayments(Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        List<PaymentResponse> responses = paymentService.getMemberPayments(memberId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .map(a -> a.substring(5))
                .orElse("MEMBER");
        PaymentResponse response = paymentService.getPayment(id, requesterId, role);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{id}/succeed")
    public ResponseEntity<PaymentResponse> succeedPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{id}/fail")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable UUID id,
            @Valid @RequestBody FailPaymentRequest request) {
        PaymentResponse response = paymentService.failPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices/{policyId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByPolicyId(@PathVariable UUID policyId) {
        List<InvoiceResponse> responses = paymentService.getInvoicesByPolicyId(policyId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            Authentication authentication,
            @Valid @RequestBody CreateRefundRequest request) {
        UUID memberId = UUID.fromString(authentication.getName());
        RefundResponse response = paymentService.createRefund(memberId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/refunds/me")
    public ResponseEntity<List<RefundResponse>> getMemberRefunds(Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        List<RefundResponse> responses = paymentService.getMemberRefunds(memberId);
        return ResponseEntity.ok(responses);
    }
}
