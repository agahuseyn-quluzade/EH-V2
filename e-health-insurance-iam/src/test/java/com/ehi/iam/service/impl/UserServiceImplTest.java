package com.ehi.iam.service.impl;

import com.ehi.iam.dto.request.ChangePasswordRequest;
import com.ehi.iam.dto.request.ChangeRoleRequest;
import com.ehi.iam.dto.request.ChangeStatusRequest;
import com.ehi.iam.dto.response.UserDto;
import com.ehi.iam.entity.User;
import com.ehi.iam.mapper.UserMapper;
import com.ehi.iam.repository.UserRepository;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.UserRole;
import com.ehi.infra.exception.base.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserServiceImpl userService;

    private User sampleUser() {
        return User.builder()
                .id(UUID.randomUUID()).email("test@example.com").password("hashed")
                .firstName("John").lastName("Doe").role(UserRole.CUSTOMER).active(true)
                .build();
    }

    private UserDto sampleDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getRole(), Instant.now(), user.getActive());
    }

    @Test
    void changePassword_throwsBadRequest_whenCurrentPasswordWrong() {
        User user = sampleUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(
                user.getEmail(), new ChangePasswordRequest("wrong", "newpass123")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changePassword_encodesNewPassword() {
        User user = sampleUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentpass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("newHashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(sampleDto(user));

        userService.changePassword(user.getEmail(), new ChangePasswordRequest("currentpass", "newpass123"));

        verify(passwordEncoder).encode("newpass123");
        assertThat(user.getPassword()).isEqualTo("newHashed");
    }

    @Test
    void changeRole_throwsNotFound_whenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(id, new ChangeRoleRequest(UserRole.STAFF)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void changeStatus_throwsNotFound_whenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeStatus(id, new ChangeStatusRequest(false)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchUsers_returnsPagedResponse() {
        User user = sampleUser();
        Pageable pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.search("john", pageable)).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(sampleDto(user));

        PagedResponse<UserDto> result = userService.searchUsers("john", pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
    }

    @Test
    void getCurrentUser_throwsNotFound_whenEmailMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("missing@example.com"))
                .isInstanceOf(NotFoundException.class);
    }
}
