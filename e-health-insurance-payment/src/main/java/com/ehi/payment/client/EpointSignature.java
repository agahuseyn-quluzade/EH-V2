package com.ehi.payment.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class EpointSignature {

    private EpointSignature() {
    }

    public static String sign(String privateKey, String data) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest((privateKey + data + privateKey).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    public static boolean verify(String privateKey, String data, String signature) {
        return sign(privateKey, data).equals(signature);
    }
}
