package com.ehi.payment.service;

import com.ehi.infra.exception.BadRequestException;
import com.ehi.payment.config.EpointProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EpointSignatureService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final EpointProperties properties;
    private final ObjectMapper objectMapper;

    public SignedEpointPayload sign(Map<String, Object> payload) {
        String data = encodeData(payload);
        return new SignedEpointPayload(data, signData(data));
    }

    public String encodeData(Map<String, Object> payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            return Base64.getEncoder().encodeToString(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize Epoint payload", ex);
        }
    }

    public String signData(String data) {
        return Base64.getEncoder().encodeToString(sha1(properties.getPrivateKey() + data + properties.getPrivateKey()));
    }

    public boolean verify(String data, String signature) {
        if (data == null || signature == null) {
            return false;
        }

        byte[] expected = signData(data).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    public Map<String, Object> decodeData(String data) {
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (IllegalArgumentException | IOException ex) {
            throw new BadRequestException("Invalid Epoint callback data");
        }
    }

    private byte[] sha1(String value) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 algorithm is not available", ex);
        }
    }

    public record SignedEpointPayload(String data, String signature) {
    }
}
