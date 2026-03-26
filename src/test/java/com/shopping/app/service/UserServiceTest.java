package com.shopping.app.service;

import com.shopping.app.dto.request.ChangePasswordRequest;
import com.shopping.app.dto.request.RegisterRequest;
import com.shopping.app.dto.request.UpdateProfileRequest;
import com.shopping.app.dto.response.UserResponse;
import com.shopping.app.entity.Role;
import com.shopping.app.entity.User;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.RoleRepository;
import com.shopping.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private Role userRole;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
                .id(userId)
                .email("user@test.com")
                .firstName("Test")
                .lastName("User")
                .password("encoded-password")
                .phone("+358401234567")
                .emailVerified(false)
                .enabled(true)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registerRequest = RegisterRequest.builder()
                .email("user@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .phone("+358401234567")
                .build();
    }

    @Nested
    @DisplayName("Register User")
    class RegisterUser {

        @Test
        @DisplayName("Should register user successfully")
        void register_Success() {
            // Arrange
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded-password");
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            UserResponse response = userService.register(registerRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("user@test.com");
            assertThat(response.getFirstName()).isEqualTo("Test");
            assertThat(response.getLastName()).isEqualTo("User");
            verify(userRepository).existsByEmail("user@test.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_DuplicateEmail_ThrowsException() {
            // Arrange
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Get User By ID")
    class GetUserById {

        @Test
        @DisplayName("Should return user when found")
        void getUserById_Found() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // Act
            UserResponse response = userService.getUserById(userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getEmail()).isEqualTo("user@test.com");
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void getUserById_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Get User By Email")
    class GetUserByEmail {

        @Test
        @DisplayName("Should return user when found by email")
        void getUserByEmail_Found() {
            // Arrange
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

            // Act
            UserResponse response = userService.getUserByEmail("user@test.com");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("user@test.com");
            verify(userRepository).findByEmail("user@test.com");
        }
    }

    @Nested
    @DisplayName("Update Profile")
    class UpdateProfile {

        @Test
        @DisplayName("Should update profile successfully")
        void updateProfile_Success() {
            // Arrange
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Updated")
                    .lastName("Name")
                    .phone("+358409999999")
                    .build();

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            UserResponse response = userService.updateProfile("user@test.com", request);

            // Assert
            assertThat(response).isNotNull();
            verify(userRepository).findByEmail("user@test.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateProfile_UserNotFound_ThrowsException() {
            // Arrange
            UpdateProfileRequest request = UpdateProfileRequest.builder().firstName("Updated").build();
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile("unknown@test.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("Change Password")
    class ChangePassword {

        @Test
        @DisplayName("Should change password successfully")
        void changePassword_Success() {
            // Arrange
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newpassword123")
                    .build();

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
            when(passwordEncoder.encode("newpassword123")).thenReturn("new-encoded-password");

            // Act
            userService.changePassword("user@test.com", request);

            // Assert
            verify(passwordEncoder).matches("password123", "encoded-password");
            verify(passwordEncoder).encode("newpassword123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when current password is wrong")
        void changePassword_WrongPassword_ThrowsException() {
            // Arrange
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("wrongpassword")
                    .newPassword("newpassword123")
                    .build();

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encoded-password")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.changePassword("user@test.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("password");

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
