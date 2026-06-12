package com.ehi.iam.service;

import com.ehi.iam.dto.request.UpdateUserRequest;
import com.ehi.iam.dto.response.UserDto;
import com.ehi.infra.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserDto getCurrentUser(String email);

    UserDto updateProfile(String email, UpdateUserRequest request);

    PagedResponse<UserDto> getAllUsers(Pageable pageable);

    UserDto getUserById(UUID id);
}
