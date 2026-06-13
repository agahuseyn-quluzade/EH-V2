package com.ehi.iam.service.impl;

import com.ehi.iam.dto.request.LoginRequest;
import com.ehi.iam.dto.request.RefreshRequest;
import com.ehi.iam.dto.request.RegisterRequest;
import com.ehi.iam.dto.response.AuthResponse;
import com.ehi.iam.entity.User;
import com.ehi.iam.kafka.UserRegisteredEventProducer;
import com.ehi.iam.repository.UserRepository;
import com.ehi.iam.security.JwtProvider;
import com.ehi.iam.service.AuthService;
import com.ehi.infra.enums.UserRole;
import com.ehi.infra.event.UserRegisteredEvent;
import com.ehi.infra.exception.DuplicateResourceException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.infra.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRegisteredEventProducer userRegisteredEventProducer;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(UserRole.CUSTOMER)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user userId={}, email={}", user.getId(), user.getEmail());

        userRegisteredEventProducer.publish(UserRegisteredEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .build());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Failed login attempt for email={}", request.email());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            log.warn("Login blocked: account suspended, userId={}", user.getId());
            throw new UnauthorizedException("Account is suspended");
        }

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        if (!jwtProvider.isTokenValid(token)) {
            log.warn("Refresh rejected: invalid token");
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtProvider.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
