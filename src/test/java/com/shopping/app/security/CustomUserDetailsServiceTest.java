package com.shopping.app.security;

import com.shopping.app.entity.Role;
import com.shopping.app.entity.User;
import com.shopping.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        Role adminRole = Role.builder()
                .id(2L)
                .name("ROLE_ADMIN")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .emailVerified(true)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Load User By Username")
    class LoadUserByUsername {

        @Test
        @DisplayName("Should return UserDetails when user is found")
        void loadUserByUsername_UserFound_ReturnsUserDetails() {
            // Arrange
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

            // Act
            UserDetails userDetails = customUserDetailsService.loadUserByUsername("user@test.com");

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("user@test.com");
            assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.getAuthorities()).hasSize(2);
            assertThat(userDetails.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
            verify(userRepository).findByEmail("user@test.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user is not found")
        void loadUserByUsername_UserNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("nonexistent@test.com");

            verify(userRepository).findByEmail("nonexistent@test.com");
        }
    }
}
