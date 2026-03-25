package com.shopping.app.controller;

import com.shopping.app.dto.response.UserResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.UserService;
import com.shopping.app.support.SecuredControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.shopping.app.support.SecurityTestHelper.roleJwt;
import static com.shopping.app.support.SecurityTestHelper.userJwt;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@SecuredControllerTest
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserResponse testUserResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUserResponse = UserResponse.builder()
                .id(userId)
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+358401234567")
                .avatarUrl("https://example.com/avatar.jpg")
                .emailVerified(true)
                .roles(List.of("ROLE_USER"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return 200 with user profile")
        void getCurrentUser_Returns200() throws Exception {
            when(userService.getUserByEmail(anyString())).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/v1/users/me")
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(userId.toString())))
                    .andExpect(jsonPath("$.data.email", is("user@test.com")))
                    .andExpect(jsonPath("$.data.firstName", is("John")))
                    .andExpect(jsonPath("$.data.lastName", is("Doe")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("Should return 200 for admin")
        void getUserById_Admin_Returns200() throws Exception {
            when(userService.getUserById(eq(userId))).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/v1/users/{id}", userId)
                            .with(roleJwt("admin@test.com", "ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(userId.toString())))
                    .andExpect(jsonPath("$.data.email", is("user@test.com")));
        }

        @Test
        @DisplayName("Should return 403 for non-admin user")
        void getUserById_NonAdmin_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", userId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isForbidden());
        }
    }
}
