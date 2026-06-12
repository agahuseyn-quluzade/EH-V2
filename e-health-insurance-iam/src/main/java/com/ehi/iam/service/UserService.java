package com.ehi.iam.service;

import com.ehi.iam.dto.request.ChangePasswordRequest;
import com.ehi.iam.dto.request.ChangeRoleRequest;
import com.ehi.iam.dto.request.ChangeStatusRequest;
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

    UserDto changePassword(String email, ChangePasswordRequest request);

    UserDto changeRole(UUID id, ChangeRoleRequest request);

    UserDto changeStatus(UUID id, ChangeStatusRequest request);

    PagedResponse<UserDto> searchUsers(String query, Pageable pageable);
}
