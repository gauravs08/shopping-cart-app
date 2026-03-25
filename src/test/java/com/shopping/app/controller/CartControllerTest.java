package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.CartItemRequest;
import com.shopping.app.dto.response.CartItemResponse;
import com.shopping.app.dto.response.CartResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import com.shopping.app.config.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIGRldmVsb3BtZW50IG9ubHk=",
        "app.cors.allowed-origins=http://localhost:3000"
})
@DisplayName("CartController Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CartResponse testCartResponse;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        CartItemResponse cartItemResponse = CartItemResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Test Product")
                .productSlug("test-product")
                .productImageUrl("https://example.com/image.jpg")
                .unitPrice(new BigDecimal("25.00"))
                .quantity(2)
                .subtotal(new BigDecimal("50.00"))
                .build();

        testCartResponse = CartResponse.builder()
                .id(UUID.randomUUID())
                .items(List.of(cartItemResponse))
                .totalItems(2)
                .totalAmount(new BigDecimal("50.00"))
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/cart")
    class GetCart {

        @Test
        @DisplayName("Should return 200 with cart")
        void getCart_Returns200() throws Exception {
            // Arrange
            when(cartService.getCart(anyString())).thenReturn(testCartResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart")
                            .with(jwt().jwt(j -> j.subject("user@test.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalItems", is(2)))
                    .andExpect(jsonPath("$.data.totalAmount", is(50.00)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cart/items")
    class AddToCart {

        @Test
        @DisplayName("Should return 201 when adding valid item to cart")
        void addToCart_ValidItem_Returns201() throws Exception {
            // Arrange
            CartItemRequest request = CartItemRequest.builder()
                    .productId(productId)
                    .quantity(2)
                    .build();

            when(cartService.addToCart(anyString(), any(CartItemRequest.class)))
                    .thenReturn(testCartResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/items")
                            .with(jwt().jwt(j -> j.subject("user@test.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 400 for invalid quantity")
        void addToCart_InvalidQuantity_Returns400() throws Exception {
            // Arrange
            CartItemRequest invalidRequest = CartItemRequest.builder()
                    .productId(productId)
                    .quantity(0)
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/items")
                            .with(jwt().jwt(j -> j.subject("user@test.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/cart/items/{productId}")
    class UpdateQuantity {

        @Test
        @DisplayName("Should return 200 when updating item quantity")
        void updateQuantity_Returns200() throws Exception {
            // Arrange
            when(cartService.updateCartItemQuantity(anyString(), eq(productId), anyInt()))
                    .thenReturn(testCartResponse);

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/items/{productId}", productId)
                            .with(jwt().jwt(j -> j.subject("user@test.com")))
                            .param("quantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cart/items/{productId}")
    class RemoveItem {

        @Test
        @DisplayName("Should return 200 when removing item from cart")
        void removeItem_Returns200() throws Exception {
            // Arrange
            when(cartService.removeFromCart(anyString(), eq(productId)))
                    .thenReturn(testCartResponse);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/items/{productId}", productId)
                            .with(jwt().jwt(j -> j.subject("user@test.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cart")
    class ClearCart {

        @Test
        @DisplayName("Should return 200 when clearing cart")
        void clearCart_Returns200() throws Exception {
            // Arrange
            CartResponse emptyCart = CartResponse.builder()
                    .id(UUID.randomUUID())
                    .items(List.of())
                    .totalItems(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            when(cartService.clearCart(anyString())).thenReturn(emptyCart);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart")
                            .with(jwt().jwt(j -> j.subject("user@test.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }
}
