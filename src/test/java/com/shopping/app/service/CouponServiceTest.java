package com.shopping.app.service;

import com.shopping.app.dto.request.ApplyCouponRequest;
import com.shopping.app.dto.response.CouponResponse;
import com.shopping.app.entity.*;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.CartRepository;
import com.shopping.app.repository.CouponRepository;
import com.shopping.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService Tests")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    private User testUser;
    private Cart testCart;
    private Coupon percentageCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .firstName("Test")
                .lastName("User")
                .password("encoded")
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .price(BigDecimal.valueOf(100))
                .sku("SKU-001")
                .slug("test-product")
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantity(2)
                .build();

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();

        percentageCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("SAVE10")
                .description("10% off")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.TEN)
                .minOrderAmount(BigDecimal.valueOf(50))
                .maxDiscountAmount(BigDecimal.valueOf(20))
                .active(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();

        fixedCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("FLAT5")
                .description("5 EUR off")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(5))
                .minOrderAmount(BigDecimal.valueOf(30))
                .active(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Nested
    @DisplayName("Apply Coupon")
    class ApplyCoupon {

        @Test
        @DisplayName("Should apply percentage coupon successfully")
        void applyCoupon_Percentage_Success() {
            // Arrange
            ApplyCouponRequest request = ApplyCouponRequest.builder().code("SAVE10").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(percentageCoupon));

            // Act
            CouponResponse response = couponService.applyCoupon("user@test.com", request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo("SAVE10");
            assertThat(response.getDiscountAmount()).isNotNull();
            assertThat(response.getFinalTotal()).isLessThan(response.getCartTotal());
        }

        @Test
        @DisplayName("Should apply fixed coupon successfully")
        void applyCoupon_Fixed_Success() {
            // Arrange
            ApplyCouponRequest request = ApplyCouponRequest.builder().code("FLAT5").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeIgnoreCase("FLAT5")).thenReturn(Optional.of(fixedCoupon));

            // Act
            CouponResponse response = couponService.applyCoupon("user@test.com", request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
        }

        @Test
        @DisplayName("Should throw exception for invalid coupon code")
        void applyCoupon_InvalidCode_ThrowsException() {
            // Arrange
            ApplyCouponRequest request = ApplyCouponRequest.builder().code("INVALID").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> couponService.applyCoupon("user@test.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Coupon");
        }

        @Test
        @DisplayName("Should throw exception for expired coupon")
        void applyCoupon_ExpiredCoupon_ThrowsException() {
            // Arrange
            percentageCoupon.setActive(false);
            ApplyCouponRequest request = ApplyCouponRequest.builder().code("SAVE10").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(percentageCoupon));

            // Act & Assert
            assertThatThrownBy(() -> couponService.applyCoupon("user@test.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should throw exception when cart is empty")
        void applyCoupon_EmptyCart_ThrowsException() {
            // Arrange
            ApplyCouponRequest request = ApplyCouponRequest.builder().code("SAVE10").build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> couponService.applyCoupon("user@test.com", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty");
        }
    }

    @Nested
    @DisplayName("Validate Coupon")
    class ValidateCoupon {

        @Test
        @DisplayName("Should validate coupon successfully")
        void validateCoupon_Success() {
            // Arrange
            when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(percentageCoupon));

            // Act
            CouponResponse response = couponService.validateCoupon("SAVE10");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo("SAVE10");
        }

        @Test
        @DisplayName("Should throw exception for non-existent coupon")
        void validateCoupon_NotFound_ThrowsException() {
            // Arrange
            when(couponRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> couponService.validateCoupon("NOPE"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
