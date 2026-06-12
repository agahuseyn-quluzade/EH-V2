package com.ehi.payment.client;

import com.ehi.infra.exception.BadRequestException;
import com.ehi.payment.config.EpointProperties;
import com.ehi.payment.service.EpointSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class EpointClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final EpointProperties properties;
    private final EpointSignatureService signatureService;
    private final RestClient restClient;

    public EpointClient(EpointProperties properties, EpointSignatureService signatureService, RestClient.Builder builder) {
        this.properties = properties;
        this.signatureService = signatureService;
        this.restClient = builder.baseUrl(stripTrailingSlash(properties.getBaseUrl())).build();
    }

    public EpointClientResponse requestPayment(String orderId, BigDecimal amount, String description) {
        Map<String, Object> payload = basePayload();
        payload.put("amount", formatAmount(amount));
        payload.put("currency", properties.getCurrency());
        payload.put("order_id", orderId);
        payload.put("description", description);
        putIfPresent(payload, "success_redirect_url", properties.getSuccessRedirectUrl());
        putIfPresent(payload, "error_redirect_url", properties.getErrorRedirectUrl());
        putIfPresent(payload, "result_url", properties.getResultUrl());

        return post("/request", payload);
    }

    public EpointClientResponse getStatusByOrderId(String orderId) {
        Map<String, Object> payload = basePayload();
        payload.put("order_id", orderId);
        return post("/get-status", payload);
    }

    public EpointClientResponse getStatusByTransaction(String transaction) {
        Map<String, Object> payload = basePayload();
        payload.put("transaction", transaction);
        return post("/get-status", payload);
    }

    public EpointClientResponse reverse(String transaction, BigDecimal amount) {
        Map<String, Object> payload = basePayload();
        payload.put("transaction", transaction);
        payload.put("currency", properties.getCurrency());
        if (amount != null) {
            payload.put("amount", formatAmount(amount));
        }
        return post("/reverse", payload);
    }

    private EpointClientResponse post(String path, Map<String, Object> payload) {
        EpointSignatureService.SignedEpointPayload signed = signatureService.sign(payload);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", signed.data());
        form.add("signature", signed.signature());

        Map<String, Object> response = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(MAP_TYPE);

        if (response == null) {
            throw new BadRequestException("Empty response from Epoint");
        }

        return unwrapSignedResponse(response);
    }

    private EpointClientResponse unwrapSignedResponse(Map<String, Object> response) {
        Object data = response.get("data");
        Object signature = response.get("signature");
        if (data instanceof String rawData && signature instanceof String rawSignature) {
            if (!signatureService.verify(rawData, rawSignature)) {
                log.warn("Invalid Epoint response signature");
                throw new BadRequestException("Invalid Epoint response signature");
            }
            return new EpointClientResponse(signatureService.decodeData(rawData), rawData, rawSignature);
        }

        return new EpointClientResponse(response, null, null);
    }

    private Map<String, Object> basePayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("public_key", properties.getPublicKey());
        payload.put("language", properties.getLanguage());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://epoint.az/api/1";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
