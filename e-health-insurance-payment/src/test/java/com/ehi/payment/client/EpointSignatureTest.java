package com.ehi.payment.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EpointSignatureTest {

    // Test vector from the official Epoint API doc (v1.0.3, "Checking payment status")
    private static final String PRIVATE_KEY = "d3hjsl38sd8kdfhbcea0be04eafde9e8e2bad2fb092d";
    private static final String DATA = "eyJwdWJsaWNfa2V5IjoiaTAwMDAwMDAwMSIsIm9yZGVyX2lkIjoxNX0=";
    private static final String EXPECTED_SIGNATURE = "bH9cG854p/wHLf5j6pp6LBI+wBs=";

    @Test
    void sign_matchesEpointDocExample() {
        assertThat(EpointSignature.sign(PRIVATE_KEY, DATA)).isEqualTo(EXPECTED_SIGNATURE);
    }

    @Test
    void verify_acceptsValidSignature() {
        assertThat(EpointSignature.verify(PRIVATE_KEY, DATA, EXPECTED_SIGNATURE)).isTrue();
    }

    @Test
    void verify_rejectsTamperedData() {
        assertThat(EpointSignature.verify(PRIVATE_KEY, DATA + "x", EXPECTED_SIGNATURE)).isFalse();
    }

    @Test
    void verify_rejectsWrongKey() {
        assertThat(EpointSignature.verify("wrong-key", DATA, EXPECTED_SIGNATURE)).isFalse();
    }
}
