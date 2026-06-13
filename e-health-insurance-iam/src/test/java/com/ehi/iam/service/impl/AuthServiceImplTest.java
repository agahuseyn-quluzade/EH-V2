package com.ehi.iam.service.impl;

import com.ehi.iam.dto.request.LoginRequest;
import com.ehi.iam.dto.request.RefreshRequest;
import com.ehi.iam.dto.request.RegisterRequest;
import com.ehi.iam.dto.response.AuthResponse;
import com.ehi.iam.entity.User;
import com.ehi.iam.kafka.UserRegisteredEventProducer;
import com.ehi.iam.repository.UserRepository;
import com.ehi.iam.security.JwtProvider;
import com.ehi.infra.enums.UserRole;
import com.ehi.infra.event.UserRegisteredEvent;
import com.ehi.infra.exception.DuplicateResourceException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.infra.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @Mock UserRegisteredEventProducer userRegisteredEventProducer;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void register_hashesPassword_andDefaultsToCustomer() {
        var request = new RegisterRequest("test@example.com", "password123", "John", "Doe", "+994501234567");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");

        User saved = User.builder()
                .id(UUID.randomUUID()).email(request.email()).password("hashed")
                .firstName("John").lastName("Doe").role(UserRole.CUSTOMER).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtProvider.generateAccessToken(any(), anyString(), any())).thenReturn("access");
        when(jwtProvider.generateRefreshToken(any(), anyString(), any())).thenReturn("refresh");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void register_throwsDuplicateResourceException_whenEmailExists() {
        var request = new RegisterRequest("exists@example.com", "password123", "Jane", "Doe", "+994501234568");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void register_publishesUserRegisteredEvent() {
        var request = new RegisterRequest("new@example.com", "password123", "New", "User", "+994501234569");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User saved = User.builder()
                .id(UUID.randomUUID()).email(request.email())
                .firstName("New").lastName("User").role(UserRole.CUSTOMER).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtProvider.generateAccessToken(any(), anyString(), any())).thenReturn("access");
        when(jwtProvider.generateRefreshToken(any(), anyString(), any())).thenReturn("refresh");

        authService.register(request);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(userRegisteredEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().email()).isEqualTo(request.email());
    }

    @Test
    void login_throwsUnauthorized_whenEmailNotFound() {
        var request = new LoginRequest("unknown@example.com", "password");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenPasswordWrong() {
        var request = new LoginRequest("test@example.com", "wrong");
        User user = User.builder().email(request.email()).password("hashed").role(UserRole.CUSTOMER).build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenUserSuspended() {
        var request = new LoginRequest("suspended@example.com", "password");
        User user = User.builder()
                .email(request.email()).password("hashed")
                .role(UserRole.CUSTOMER).active(false).build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void login_succeeds_forActiveUser() {
        var request = new LoginRequest("active@example.com", "password");
        User user = User.builder()
                .id(UUID.randomUUID()).email(request.email()).password("hashed")
                .role(UserRole.CUSTOMER).active(true).build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken(any(), anyString(), any())).thenReturn("access");
        when(jwtProvider.generateRefreshToken(any(), anyString(), any())).thenReturn("refresh");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.email()).isEqualTo(request.email());
    }

    @Test
    void refresh_throwsUnauthorized_whenTokenInvalid() {
        when(jwtProvider.isTokenValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_throwsNotFound_whenUserMissing() {
        UUID id = UUID.randomUUID();
        when(jwtProvider.isTokenValid("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(id);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("valid-token")))
                .isInstanceOf(NotFoundException.class);
    }
}
