package com.ehi.iam.service.impl;

import com.ehi.iam.dto.request.ChangePasswordRequest;
import com.ehi.iam.dto.request.ChangeRoleRequest;
import com.ehi.iam.dto.request.ChangeStatusRequest;
import com.ehi.iam.dto.request.UpdateUserRequest;
import com.ehi.iam.dto.response.UserDto;
import com.ehi.iam.entity.User;
import com.ehi.iam.mapper.UserMapper;
import com.ehi.iam.repository.UserRepository;
import com.ehi.iam.service.UserService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.exception.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));
        return userMapper.toDto(user);
    }

    @Override
    public UserDto updateProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public PagedResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);

        return PagedResponse.<UserDto>builder()
                .content(page.getContent().stream().map(userMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        return userMapper.toDto(user);
    }

    @Override
    public UserDto changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto changeRole(UUID id, ChangeRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        user.setRole(request.role());
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto changeStatus(UUID id, ChangeStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        user.setActive(request.active());
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public PagedResponse<UserDto> searchUsers(String query, Pageable pageable) {
        Page<User> page = userRepository.search(query, pageable);
        return PagedResponse.<UserDto>builder()
                .content(page.getContent().stream().map(userMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
