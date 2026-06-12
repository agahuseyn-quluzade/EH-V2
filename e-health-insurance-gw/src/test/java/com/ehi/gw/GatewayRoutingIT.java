package com.ehi.gw;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIT {

    private static final String JWT_SECRET = "change-me-to-a-secure-256-bit-secret-key-for-jwt-signing-please";

    private static WireMockServer wireMockServer;

    @Autowired
    WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        registry.add("spring.cloud.gateway.routes[0].id", () -> "auth");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> wireMockServer.baseUrl());
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/v1/auth/**");

        registry.add("spring.cloud.gateway.routes[1].id", () -> "claims");
        registry.add("spring.cloud.gateway.routes[1].uri", () -> wireMockServer.baseUrl());
        registry.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/api/v1/claims/**");
    }

    @AfterAll
    static void tearDownAll() {
        wireMockServer.stop();
    }

    @AfterEach
    void resetStubs() {
        wireMockServer.resetAll();
    }

    private String validToken(UUID userId, String role) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@example.com")
                .claim("userId", userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(900_000L)))
                .signWith(key)
                .compact();
    }

    @Test
    void publicAuthRoute_forwardsWithoutToken_toDownstreamService() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse().withStatus(200).withBody("ok")));

        webTestClient.get().uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk();

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/auth/login")));
    }

    @Test
    void protectedRoute_withoutToken_returnsUnauthorized_andNeverReachesDownstream() {
        webTestClient.get().uri("/api/v1/claims/123")
                .exchange()
                .expectStatus().isUnauthorized();

        wireMockServer.verify(0, getRequestedFor(urlEqualTo("/api/v1/claims/123")));
    }

    @Test
    void protectedRoute_withValidToken_forwardsRequest_withUserHeaders() {
        UUID userId = UUID.randomUUID();
        String token = validToken(userId, "CUSTOMER");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/claims/123"))
                .willReturn(aResponse().withStatus(200).withBody("claim")));

        webTestClient.get().uri("/api/v1/claims/123")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/claims/123"))
                .withHeader("X-User-Id", equalTo(userId.toString()))
                .withHeader("X-User-Role", equalTo("CUSTOMER")));
    }
}
