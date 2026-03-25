package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.ReviewRequest;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.ReviewResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.ReviewService;
import com.shopping.app.support.SecuredControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.shopping.app.support.SecurityTestHelper.userJwt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@SecuredControllerTest
@DisplayName("ReviewController Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ReviewResponse testReviewResponse;
    private ReviewRequest testReviewRequest;
    private UUID productId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        testReviewResponse = ReviewResponse.builder()
                .id(reviewId)
                .productId(productId)
                .productName("Test Product")
                .userId(UUID.randomUUID())
                .userName("Test User")
                .rating(5)
                .title("Great product")
                .comment("Really enjoyed using this product")
                .verifiedPurchase(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testReviewRequest = ReviewRequest.builder()
                .rating(5)
                .title("Great product")
                .comment("Really enjoyed using this product")
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/reviews/products/{productId}")
    class CreateReview {

        @Test
        @DisplayName("Should return 201 when creating a valid review")
        void createReview_Returns201() throws Exception {
            when(reviewService.createReview(eq(productId), anyString(), any(ReviewRequest.class)))
                    .thenReturn(testReviewResponse);

            mockMvc.perform(post("/api/v1/reviews/products/{productId}", productId)
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testReviewRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.rating", is(5)))
                    .andExpect(jsonPath("$.data.title", is("Great product")))
                    .andExpect(jsonPath("$.data.productId", is(productId.toString())));
        }

        @Test
        @DisplayName("Should return 400 when rating is invalid")
        void createReview_InvalidRating_Returns400() throws Exception {
            ReviewRequest invalidRequest = ReviewRequest.builder()
                    .rating(0)
                    .title("Bad rating")
                    .comment("This should fail validation")
                    .build();

            mockMvc.perform(post("/api/v1/reviews/products/{productId}", productId)
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/products/{productId}")
    class GetProductReviews {

        @Test
        @DisplayName("Should return 200 with paginated product reviews")
        void getProductReviews_Returns200() throws Exception {
            PagedResponse<ReviewResponse> pagedResponse = PagedResponse.<ReviewResponse>builder()
                    .content(List.of(testReviewResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(reviewService.getReviewsByProduct(eq(productId), any(Pageable.class)))
                    .thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/reviews/products/{productId}", productId)
                            .with(userJwt("user@test.com"))
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].rating", is(5)))
                    .andExpect(jsonPath("$.data.totalElements", is(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/my-reviews")
    class GetMyReviews {

        @Test
        @DisplayName("Should return 200 with user's reviews")
        void getMyReviews_Returns200() throws Exception {
            PagedResponse<ReviewResponse> pagedResponse = PagedResponse.<ReviewResponse>builder()
                    .content(List.of(testReviewResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(reviewService.getReviewsByUser(anyString(), any(Pageable.class)))
                    .thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/reviews/my-reviews")
                            .with(userJwt("user@test.com"))
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].title", is("Great product")))
                    .andExpect(jsonPath("$.data.totalElements", is(1)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/reviews/{id}")
    class DeleteReview {

        @Test
        @DisplayName("Should return 200 when deleting own review")
        void deleteReview_Returns200() throws Exception {
            mockMvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }
}
