package com.shopping.app.service;

import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.WishlistResponse;
import com.shopping.app.entity.Product;
import com.shopping.app.entity.User;
import com.shopping.app.entity.Wishlist;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.ProductRepository;
import com.shopping.app.repository.UserRepository;
import com.shopping.app.repository.WishlistRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService Tests")
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Product testProduct;
    private Wishlist testWishlist;
    private UUID userId;
    private UUID productId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userEmail = "user@test.com";

        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .firstName("Test")
                .lastName("User")
                .password("encoded-password")
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .sku("TEST-001")
                .price(new BigDecimal("29.99"))
                .imageUrl("https://example.com/image.jpg")
                .stockQuantity(10)
                .active(true)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testWishlist = Wishlist.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .product(testProduct)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Wishlist")
    class GetWishlist {

        @Test
        @DisplayName("Should return paginated wishlist for user")
        void getWishlist_ReturnsPagedResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20);
            Page<Wishlist> wishlistPage = new PageImpl<>(List.of(testWishlist), pageable, 1);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(wishlistPage);

            // Act
            PagedResponse<WishlistResponse> response = wishlistService.getWishlist(userEmail, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getProductName()).isEqualTo("Test Product");
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.isLast()).isTrue();
            verify(wishlistRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
    }

    @Nested
    @DisplayName("Add To Wishlist")
    class AddToWishlist {

        @Test
        @DisplayName("Should add product to wishlist successfully")
        void addToWishlist_Success() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(wishlistRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

            // Act
            WishlistResponse response = wishlistService.addToWishlist(userEmail, productId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getProductName()).isEqualTo("Test Product");
            assertThat(response.isInStock()).isTrue();
            verify(wishlistRepository).save(any(Wishlist.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when product already in wishlist")
        void addToWishlist_Duplicate_ThrowsBadRequestException() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(wishlistRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> wishlistService.addToWishlist(userEmail, productId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already in your wishlist");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product not found")
        void addToWishlist_ProductNotFound_ThrowsException() {
            // Arrange
            UUID nonExistentProductId = UUID.randomUUID();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> wishlistService.addToWishlist(userEmail, nonExistentProductId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }
    }

    @Nested
    @DisplayName("Remove From Wishlist")
    class RemoveFromWishlist {

        @Test
        @DisplayName("Should remove product from wishlist successfully")
        void removeFromWishlist_Success() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(wishlistRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.of(testWishlist));

            // Act
            wishlistService.removeFromWishlist(userEmail, productId);

            // Assert
            verify(wishlistRepository).delete(testWishlist);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when wishlist entry not found")
        void removeFromWishlist_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentProductId = UUID.randomUUID();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(wishlistRepository.findByUserIdAndProductId(userId, nonExistentProductId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> wishlistService.removeFromWishlist(userEmail, nonExistentProductId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wishlist");
        }
    }

    @Nested
    @DisplayName("Is In Wishlist")
    class IsInWishlist {

        @Test
        @DisplayName("Should return true when product is in wishlist")
        void isInWishlist_ReturnsTrue() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(wishlistRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

            // Act
            boolean result = wishlistService.isInWishlist(userEmail, productId);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when product is not in wishlist")
        void isInWishlist_ReturnsFalse() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(wishlistRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);

            // Act
            boolean result = wishlistService.isInWishlist(userEmail, productId);

            // Assert
            assertThat(result).isFalse();
        }
    }
}
