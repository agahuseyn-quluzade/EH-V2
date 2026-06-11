package com.eHealthInsurance.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@TestPropertySource(properties = {
        "ehi.security.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctcHVycG9zZXMtb25seQ==",
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctcHVycG9zZXMtb25seQ==",
        "spring.datasource.url=jdbc:h2:mem:healthrecord-security",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "ehi.outbox.publisher.enabled=false"
})
class HealthRecordSecurityIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void memberCanReachOwnRecordEndpoint() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/records/me").with(jwt()
                        .jwt(jwt -> jwt.subject(memberId.toString()).claim("user_id", memberId.toString()))
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotUseMemberSelfEndpoint() throws Exception {
        UUID staffId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/records/me").with(jwt()
                        .jwt(jwt -> jwt.subject(staffId.toString()).claim("user_id", staffId.toString()))
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_STAFF")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCannotReadAnotherMembersRecord() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/records/members/{memberId}", otherMemberId).with(jwt()
                        .jwt(jwt -> jwt.subject(memberId.toString()).claim("user_id", memberId.toString()))
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanReachMemberReadEndpoint() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/records/members/{memberId}", memberId).with(jwt()
                        .jwt(jwt -> jwt.subject(staffId.toString()).claim("user_id", staffId.toString()))
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_STAFF")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void memberCanReachOwnStatusUpdateEndpoint() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/records/me/status")
                        .contentType("application/json")
                        .content("{\"status\":\"ARCHIVED\"}")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(memberId.toString()).claim("user_id", memberId.toString()))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedStatusUpdateBodyReturnsBadRequest() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/records/me/status")
                        .contentType("application/json")
                        .content("{\"status\":ARCHIVED}")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(memberId.toString()).claim("user_id", memberId.toString()))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void memberCannotDeleteAnotherMembersRecord() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/records/members/{memberId}", otherMemberId).with(jwt()
                        .jwt(jwt -> jwt.subject(memberId.toString()).claim("user_id", memberId.toString()))
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanReachMemberStatusUpdateEndpoint() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/records/members/{memberId}/status", memberId)
                        .contentType("application/json")
                        .content("{\"status\":\"ARCHIVED\"}")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject(staffId.toString()).claim("user_id", staffId.toString()))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STAFF")))))
                .andExpect(status().isNotFound());
    }
}
