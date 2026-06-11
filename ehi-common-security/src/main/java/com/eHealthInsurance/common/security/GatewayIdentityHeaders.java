package com.eHealthInsurance.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class GatewayIdentityHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String GATEWAY = "X-EHI-Gateway";
    public static final String TIMESTAMP = "X-EHI-Gateway-Timestamp";
    public static final String SIGNATURE = "X-EHI-Gateway-Signature";
    public static final String GATEWAY_VALUE = "e-health-insurance-gateway";

    private GatewayIdentityHeaders() {
    }

    public static String sign(String secret, String userId, String role, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload(userId, role, timestamp)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign gateway identity headers", ex);
        }
    }

    public static boolean signatureMatches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] payload(String userId, String role, String timestamp) {
        String value = nullToEmpty(userId) + "\n" + nullToEmpty(role) + "\n" + nullToEmpty(timestamp);
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
