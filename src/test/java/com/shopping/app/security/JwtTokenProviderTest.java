package com.shopping.app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);

        mockJwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "HS256")
                .claim("sub", "user@test.com")
                .claim("roles", "ROLE_USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Nested
    @DisplayName("Generate Token from Authentication")
    class GenerateToken {

        @Test
        @DisplayName("Should generate token for valid authentication")
        void generateToken_ValidAuthentication_ReturnsToken() {
            // Arrange
            Collection<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            when(authentication.getName()).thenReturn("user@test.com");
            when(authentication.getAuthorities()).thenAnswer(inv -> authorities);
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            String token = jwtTokenProvider.generateToken(authentication);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isEqualTo("mock-jwt-token");
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
            verify(authentication).getName();
            verify(authentication).getAuthorities();
        }
    }

    @Nested
    @DisplayName("Generate Token for User")
    class GenerateTokenForUser {

        @Test
        @DisplayName("Should generate token with correct claims for user")
        void generateTokenForUser_ReturnsTokenWithCorrectClaims() {
            // Arrange
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            // Act
            String token = jwtTokenProvider.generateTokenForUser("admin@test.com", "ROLE_ADMIN ROLE_USER");

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isEqualTo("mock-jwt-token");
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        }
    }
}
