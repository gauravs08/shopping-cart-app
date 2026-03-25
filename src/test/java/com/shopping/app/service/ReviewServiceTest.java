package com.shopping.app.service;

import com.shopping.app.dto.request.ReviewRequest;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.ReviewResponse;
import com.shopping.app.entity.Product;
import com.shopping.app.entity.Review;
import com.shopping.app.entity.User;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.OrderItemRepository;
import com.shopping.app.repository.ProductRepository;
import com.shopping.app.repository.ReviewRepository;
import com.shopping.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Product testProduct;
    private Review testReview;
    private ReviewRequest testReviewRequest;
    private UUID userId;
    private UUID productId;
    private UUID reviewId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        userEmail = "reviewer@test.com";

        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .firstName("Review")
                .lastName("User")
                .password("encoded-password")
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("Reviewed Product")
                .slug("reviewed-product")
                .sku("REV-001")
                .price(new BigDecimal("30.00"))
                .stockQuantity(50)
                .active(true)
                .ratingAverage(new BigDecimal("4.00"))
                .ratingCount(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testReview = Review.builder()
                .id(reviewId)
                .product(testProduct)
                .user(testUser)
                .rating(4)
                .title("Great product")
                .comment("Really enjoyed using this product")
                .verifiedPurchase(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testReviewRequest = ReviewRequest.builder()
                .rating(4)
                .title("Great product")
                .comment("Really enjoyed using this product")
                .build();
    }

    @Nested
    @DisplayName("Create Review")
    class CreateReview {

        @Test
        @DisplayName("Should create review successfully")
        void createReview_Success() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(false);
            when(orderItemRepository.existsByUserIdAndProductIdAndOrderDelivered(userId, productId)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            when(reviewRepository.calculateAverageRating(productId)).thenReturn(new BigDecimal("4.0"));
            when(reviewRepository.countByProductId(productId)).thenReturn(6);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            ReviewResponse response = reviewService.createReview(productId, userEmail, testReviewRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getRating()).isEqualTo(4);
            assertThat(response.getTitle()).isEqualTo("Great product");
            assertThat(response.getProductId()).isEqualTo(productId);
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when user already reviewed product")
        void createReview_AlreadyReviewed_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> reviewService.createReview(productId, userEmail, testReviewRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already");

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should set verified purchase flag when user has purchased the product")
        void createReview_VerifiedPurchase_SetsFlag() {
            // Arrange
            Review verifiedReview = Review.builder()
                    .id(reviewId)
                    .product(testProduct)
                    .user(testUser)
                    .rating(4)
                    .title("Great product")
                    .comment("Really enjoyed using this product")
                    .verifiedPurchase(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(false);
            when(orderItemRepository.existsByUserIdAndProductIdAndOrderDelivered(userId, productId)).thenReturn(true);
            when(reviewRepository.save(any(Review.class))).thenReturn(verifiedReview);
            when(reviewRepository.calculateAverageRating(productId)).thenReturn(new BigDecimal("4.0"));
            when(reviewRepository.countByProductId(productId)).thenReturn(6);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            ReviewResponse response = reviewService.createReview(productId, userEmail, testReviewRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isVerifiedPurchase()).isTrue();
            verify(reviewRepository).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("Get Reviews By Product")
    class GetReviewsByProduct {

        @Test
        @DisplayName("Should return paginated reviews for a product")
        void getReviewsByProduct_ReturnsResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> reviewPage = new PageImpl<>(List.of(testReview), pageable, 1);
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)).thenReturn(reviewPage);

            // Act
            PagedResponse<ReviewResponse> response = reviewService.getReviewsByProduct(productId, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getRating()).isEqualTo(4);
            assertThat(response.getTotalElements()).isEqualTo(1);
            verify(reviewRepository).findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }
    }

    @Nested
    @DisplayName("Delete Review")
    class DeleteReview {

        @Test
        @DisplayName("Should allow owner to delete their review")
        void deleteReview_OwnerCanDelete() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            when(reviewRepository.calculateAverageRating(productId)).thenReturn(new BigDecimal("3.5"));
            when(reviewRepository.countByProductId(productId)).thenReturn(4);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            reviewService.deleteReview(reviewId, userEmail);

            // Assert
            verify(reviewRepository).delete(testReview);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when non-owner tries to delete review")
        void deleteReview_NonOwner_ThrowsException() {
            // Arrange
            String otherEmail = "other@test.com";
            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(otherEmail)
                    .build();
            when(userRepository.findByEmail(otherEmail)).thenReturn(Optional.of(otherUser));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

            // Act & Assert
            assertThatThrownBy(() -> reviewService.deleteReview(reviewId, otherEmail))
                    .isInstanceOf(BadRequestException.class);

            verify(reviewRepository, never()).delete(any(Review.class));
        }
    }
}
