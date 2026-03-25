package com.shopping.app.controller;

import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.WishlistResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.WishlistService;
import com.shopping.app.support.SecuredControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.shopping.app.support.SecurityTestHelper.userJwt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@SecuredControllerTest
@DisplayName("WishlistController Tests")
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UUID productId;
    private WishlistResponse testWishlistResponse;
    private PagedResponse<WishlistResponse> testPagedResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        testWishlistResponse = WishlistResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Test Product")
                .productSlug("test-product")
                .productImageUrl("https://example.com/image.jpg")
                .productPrice(new BigDecimal("29.99"))
                .inStock(true)
                .addedAt(LocalDateTime.now())
                .build();

        testPagedResponse = PagedResponse.<WishlistResponse>builder()
                .content(List.of(testWishlistResponse))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/wishlist")
    class GetWishlist {

        @Test
        @DisplayName("Should return 200 with paginated wishlist")
        void getWishlist_Returns200() throws Exception {
            when(wishlistService.getWishlist(anyString(), any(Pageable.class)))
                    .thenReturn(testPagedResponse);

            mockMvc.perform(get("/api/v1/wishlist")
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].productName", is("Test Product")))
                    .andExpect(jsonPath("$.data.totalElements", is(1)))
                    .andExpect(jsonPath("$.data.last", is(true)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/wishlist/{productId}")
    class AddToWishlist {

        @Test
        @DisplayName("Should return 201 when adding product to wishlist")
        void addToWishlist_Returns201() throws Exception {
            when(wishlistService.addToWishlist(anyString(), eq(productId)))
                    .thenReturn(testWishlistResponse);

            mockMvc.perform(post("/api/v1/wishlist/{productId}", productId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.productId", is(productId.toString())))
                    .andExpect(jsonPath("$.data.productName", is("Test Product")))
                    .andExpect(jsonPath("$.data.inStock", is(true)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/wishlist/{productId}")
    class RemoveFromWishlist {

        @Test
        @DisplayName("Should return 200 when removing product from wishlist")
        void removeFromWishlist_Returns200() throws Exception {
            mockMvc.perform(delete("/api/v1/wishlist/{productId}", productId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            verify(wishlistService).removeFromWishlist(anyString(), eq(productId));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/wishlist/{productId}/check")
    class IsInWishlist {

        @Test
        @DisplayName("Should return true when product is in wishlist")
        void isInWishlist_ReturnsTrue() throws Exception {
            when(wishlistService.isInWishlist(anyString(), eq(productId)))
                    .thenReturn(true);

            mockMvc.perform(get("/api/v1/wishlist/{productId}/check", productId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", is(true)));
        }

        @Test
        @DisplayName("Should return false when product is not in wishlist")
        void isInWishlist_ReturnsFalse() throws Exception {
            when(wishlistService.isInWishlist(anyString(), eq(productId)))
                    .thenReturn(false);

            mockMvc.perform(get("/api/v1/wishlist/{productId}/check", productId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", is(false)));
        }
    }
}
