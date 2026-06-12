package com.ehi.payment.controller;

import com.ehi.infra.dto.ApiResponse;
import com.ehi.payment.dto.request.EpointCallbackRequest;
import com.ehi.payment.dto.request.EpointInitPaymentRequest;
import com.ehi.payment.dto.request.EpointReverseRequest;
import com.ehi.payment.dto.response.EpointPaymentResponse;
import com.ehi.payment.service.EpointPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class EpointPaymentController {

    private final EpointPaymentService epointPaymentService;

    @PostMapping("/epoint/init")
    public ResponseEntity<ApiResponse<EpointPaymentResponse>> init(
            Authentication authentication,
            @Valid @RequestBody EpointInitPaymentRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.initPayment(userId, request)));
    }

    @PostMapping(value = "/epoint/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ApiResponse<EpointPaymentResponse>> callbackForm(
            @RequestParam String data,
            @RequestParam String signature) {
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.processCallback(data, signature)));
    }

    @PostMapping(value = "/epoint/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<EpointPaymentResponse>> callbackJson(
            @Valid @RequestBody EpointCallbackRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.processCallback(request.data(), request.signature())));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EpointPaymentResponse>> status(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.getStatus(id, userId, isAdmin(authentication))));
    }

    @PostMapping("/{id}/sync-status")
    public ResponseEntity<ApiResponse<EpointPaymentResponse>> syncStatus(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.syncStatus(id, userId, isAdmin(authentication))));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<ApiResponse<EpointPaymentResponse>> reverse(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) EpointReverseRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(epointPaymentService.reverse(id, userId, isAdmin(authentication), request)));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }
}
