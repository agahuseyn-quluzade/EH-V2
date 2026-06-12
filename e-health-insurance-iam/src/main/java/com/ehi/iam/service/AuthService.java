package com.ehi.iam.service;

import com.ehi.iam.dto.request.LoginRequest;
import com.ehi.iam.dto.request.RefreshRequest;
import com.ehi.iam.dto.request.RegisterRequest;
import com.ehi.iam.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);
}
