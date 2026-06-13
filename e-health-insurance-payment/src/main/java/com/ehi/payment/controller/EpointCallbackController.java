package com.ehi.payment.controller;

import com.ehi.payment.service.EpointPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/epoint")
@RequiredArgsConstructor
public class EpointCallbackController {

    private final EpointPaymentService epointPaymentService;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam String data, @RequestParam String signature) {
        epointPaymentService.handleCallback(data, signature);
        return ResponseEntity.ok().build();
    }
}
