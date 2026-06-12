package com.ehi.iam.controller;

import com.ehi.iam.dto.request.ChangePasswordRequest;
import com.ehi.iam.dto.request.ChangeRoleRequest;
import com.ehi.iam.dto.request.ChangeStatusRequest;
import com.ehi.iam.dto.request.UpdateUserRequest;
import com.ehi.iam.dto.response.UserDto;
import com.ehi.iam.service.UserService;
import com.ehi.infra.dto.ApiResponse;
import com.ehi.infra.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getCurrentUser(authentication.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(Authentication authentication,
                                                                @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(authentication.getName(), request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PostMapping("/me/password")
    public ResponseEntity<ApiResponse<UserDto>> changePassword(Authentication authentication,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.changePassword(authentication.getName(), request)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> changeRole(@PathVariable UUID id,
                                                           @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.changeRole(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> changeStatus(@PathVariable UUID id,
                                                             @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.changeStatus(id, request)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> searchUsers(@RequestParam String query,
                                                                           Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.searchUsers(query, pageable)));
    }
}
