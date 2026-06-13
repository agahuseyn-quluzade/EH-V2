package com.ehi.payment.controller;

import com.ehi.payment.dto.request.ProcessPaymentRequest;
import com.ehi.payment.dto.response.CardRegistrationResponse;
import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.dto.response.SavedCardDto;
import com.ehi.payment.service.EpointPaymentService;
import com.ehi.payment.service.PaymentService;
import com.ehi.infra.dto.ApiResponse;
import com.ehi.infra.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final EpointPaymentService epointPaymentService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.processPayment(
                request.userId(), request.referenceId(), request.referenceType(), request.amount())));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getMyPayments(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getMyPayments(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentById(id, userId, isAdmin(authentication))));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentDto>>> getAllPayments(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getAllPayments(pageable)));
    }

    @PostMapping("/{id}/refresh-status")
    public ResponseEntity<ApiResponse<PaymentDto>> refreshStatus(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.refreshStatus(id, userId, isAdmin(authentication))));
    }

    @PostMapping("/cards/register")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CardRegistrationResponse>> registerCard(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.startCardRegistration(userId)));
    }

    @GetMapping("/cards/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<SavedCardDto>>> getMyCards(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.getMyCards(userId)));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }
}
