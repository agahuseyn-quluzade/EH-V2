package com.eHealthInsurance.controller;

import com.eHealthInsurance.dto.response.PaymentResponse;
import com.eHealthInsurance.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    // Additional internal endpoints for service-to-service communication can be added here.
    // The current specification defines a placeholder for internal payment status queries.

    /**
     * Internal endpoint for policy service to query payment status.
     * This is a placeholder — extend as needed for service-to-service integration.
     */
    @GetMapping("/payments/status")
    public ResponseEntity<String> getPaymentStatus() {
        return ResponseEntity.ok("payment-service-active");
    }
}
