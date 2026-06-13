package com.ehi.iam;

import com.ehi.iam.dto.request.LoginRequest;
import com.ehi.iam.dto.request.RegisterRequest;
import com.ehi.iam.kafka.UserRegisteredEventProducer;
import com.ehi.iam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class IamAuthIT {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;

    @MockBean UserRegisteredEventProducer userRegisteredEventProducer;

    @Test
    void register_login_thenAccessProfile() {
        var reg = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("it-flow@example.com", "password123", "IT", "User", null),
                Map.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.OK);

        var login = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("it-flow@example.com", "password123"),
                Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        String accessToken = extractAccessToken(login);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> me = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) me.getBody().get("data");
        assertThat(data.get("email")).isEqualTo("it-flow@example.com");
    }

    @Test
    void suspendedUser_cannotLogin() {
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("suspended-it@example.com", "password123", "Sus", "User", null),
                Map.class);

        userRepository.findByEmail("suspended-it@example.com").ifPresent(u -> {
            u.setActive(false);
            userRepository.save(u);
        });

        var login = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("suspended-it@example.com", "password123"),
                Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @SuppressWarnings("unchecked")
    private String extractAccessToken(ResponseEntity<Map> resp) {
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        return (String) data.get("accessToken");
    }
}
