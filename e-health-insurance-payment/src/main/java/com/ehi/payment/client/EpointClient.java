package com.ehi.payment.client;

import com.ehi.payment.config.EpointProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpointClient {

    private final WebClient epointWebClient;
    private final EpointProperties epointProperties;
    private final ObjectMapper objectMapper;

    public EpointCheckoutResponse createPayment(String orderId, BigDecimal amount, String description) {
        EpointPaymentRequest request = EpointPaymentRequest.builder()
                .publicKey(epointProperties.getPublicKey())
                .amount(amount)
                .currency(epointProperties.getCurrency())
                .language(epointProperties.getLanguage())
                .orderId(orderId)
                .description(description)
                .successRedirectUrl(epointProperties.getSuccessRedirectUrl())
                .errorRedirectUrl(epointProperties.getErrorRedirectUrl())
                .build();

        return post("/request", request, EpointCheckoutResponse.class);
    }

    public EpointPaymentResult getStatus(String transaction) {
        EpointStatusRequest request = new EpointStatusRequest(epointProperties.getPublicKey(), transaction);

        return post("/get-status", request, EpointPaymentResult.class);
    }

    public EpointCheckoutResponse registerPayoutCard(String description) {
        EpointCardRegistrationRequest request = EpointCardRegistrationRequest.builder()
                .publicKey(epointProperties.getPublicKey())
                .language(epointProperties.getLanguage())
                .refund(1)
                .description(description)
                .successRedirectUrl(epointProperties.getSuccessRedirectUrl())
                .errorRedirectUrl(epointProperties.getErrorRedirectUrl())
                .build();

        return post("/card-registration", request, EpointCheckoutResponse.class);
    }

    public EpointPayoutResponse payout(String cardId, String orderId, BigDecimal amount, String description) {
        EpointPayoutRequest request = EpointPayoutRequest.builder()
                .publicKey(epointProperties.getPublicKey())
                .language(epointProperties.getLanguage())
                .cardId(cardId)
                .orderId(orderId)
                .amount(amount)
                .currency(epointProperties.getCurrency())
                .description(description)
                .build();

        return post("/refund-request", request, EpointPayoutResponse.class);
    }

    public EpointReverseResponse reverse(String transaction, BigDecimal amount) {
        EpointReverseRequest request = EpointReverseRequest.builder()
                .publicKey(epointProperties.getPublicKey())
                .language(epointProperties.getLanguage())
                .transaction(transaction)
                .currency(epointProperties.getCurrency())
                .amount(amount)
                .build();

        return post("/reverse", request, EpointReverseResponse.class);
    }

    private <T> T post(String path, Object payload, Class<T> responseType) {
        String data = encode(payload);
        String signature = EpointSignature.sign(epointProperties.getPrivateKey(), data);

        try {
            return epointWebClient.post()
                    .uri(path)
                    .body(BodyInserters.fromFormData("data", data).with("signature", signature))
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofSeconds(epointProperties.getTimeoutSeconds()))
                    .block();
        } catch (Exception e) {
            log.warn("Epoint call failed: path={}", path, e);
            throw e;
        }
    }

    private String encode(Object payload) {
        try {
            return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Epoint payload", e);
        }
    }
}
