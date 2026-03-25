package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.LoginRequest;
import com.shopping.app.dto.request.RegisterRequest;
import com.shopping.app.dto.response.AuthResponse;
import com.shopping.app.dto.response.UserResponse;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.service.AuthService;
import com.shopping.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        registerRequest = RegisterRequest.builder()
                .email("newuser@test.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .phone("+358401234567")
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiJ9.test-token")
                .tokenType("Bearer")
                .expiresIn(3600000)
                .email("user@test.com")
                .roles(List.of("ROLE_USER"))
                .build();
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("Should return 200 with token for valid credentials")
        void login_ValidCredentials_Returns200() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.data.email", is("user@test.com")));
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        void login_InvalidCredentials_Returns401() throws Exception {
            // Arrange
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("Should return 201 for valid registration data")
        void register_ValidData_Returns201() throws Exception {
            // Arrange
            UserResponse userResponse = UserResponse.builder()
                    .id(UUID.randomUUID())
                    .email("newuser@test.com")
                    .firstName("New")
                    .lastName("User")
                    .roles(List.of("ROLE_USER"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(userService.register(any(RegisterRequest.class))).thenReturn(userResponse);

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email", is("newuser@test.com")));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        void register_InvalidEmail_Returns400() throws Exception {
            // Arrange
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("not-an-email")
                    .password("password123")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for duplicate email")
        void register_DuplicateEmail_Returns400() throws Exception {
            // Arrange
            when(userService.register(any(RegisterRequest.class)))
                    .thenThrow(new BadRequestException("Email already in use"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
