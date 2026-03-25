package com.shopping.app.service;

import com.shopping.app.dto.request.CartItemRequest;
import com.shopping.app.dto.response.CartResponse;
import com.shopping.app.entity.Cart;
import com.shopping.app.entity.CartItem;
import com.shopping.app.entity.Product;
import com.shopping.app.entity.User;
import com.shopping.app.exception.InsufficientStockException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.CartItemRepository;
import com.shopping.app.repository.CartRepository;
import com.shopping.app.repository.ProductRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;
    private UUID productId;
    private UUID cartId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartId = UUID.randomUUID();
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
                .price(new BigDecimal("25.00"))
                .stockQuantity(10)
                .active(true)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testCart = Cart.builder()
                .id(cartId)
                .user(testUser)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testCartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Cart")
    class GetCart {

        @Test
        @DisplayName("Should return existing cart for user")
        void getCart_ExistingCart_ReturnsCartResponse() {
            // Arrange
            testCart.getItems().add(testCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

            // Act
            CartResponse response = cartService.getCart(userEmail);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(cartId);
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            verify(cartRepository).findByUser(testUser);
        }

        @Test
        @DisplayName("Should create new cart when none exists for user")
        void getCart_NoCart_CreatesNewCart() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartResponse response = cartService.getCart(userEmail);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("Add To Cart")
    class AddToCart {

        @Test
        @DisplayName("Should add new item to cart successfully")
        void addToCart_NewItem_AddsSuccessfully() {
            // Arrange
            CartItemRequest request = CartItemRequest.builder()
                    .productId(productId)
                    .quantity(2)
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartResponse response = cartService.addToCart(userEmail, request);

            // Assert
            assertThat(response).isNotNull();
            verify(productRepository).findById(productId);
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should increment quantity when item already in cart")
        void addToCart_ExistingItem_IncrementsQuantity() {
            // Arrange
            CartItemRequest request = CartItemRequest.builder()
                    .productId(productId)
                    .quantity(1)
                    .build();

            testCart.getItems().add(testCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(testCartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartResponse response = cartService.addToCart(userEmail, request);

            // Assert
            assertThat(response).isNotNull();
            verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void addToCart_ProductNotFound_ThrowsException() {
            // Arrange
            UUID nonExistentProductId = UUID.randomUUID();
            CartItemRequest request = CartItemRequest.builder()
                    .productId(nonExistentProductId)
                    .quantity(1)
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.addToCart(userEmail, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }

        @Test
        @DisplayName("Should throw exception when insufficient stock")
        void addToCart_InsufficientStock_ThrowsException() {
            // Arrange
            CartItemRequest request = CartItemRequest.builder()
                    .productId(productId)
                    .quantity(100)
                    .build();

            Product lowStockProduct = Product.builder()
                    .id(productId)
                    .name("Low Stock Product")
                    .slug("low-stock")
                    .sku("LOW-001")
                    .price(new BigDecimal("10.00"))
                    .stockQuantity(2)
                    .active(true)
                    .ratingAverage(BigDecimal.ZERO)
                    .ratingCount(0)
                    .build();

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(lowStockProduct));
            when(cartItemRepository.findByCartAndProduct(testCart, lowStockProduct)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.addToCart(userEmail, request))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Nested
    @DisplayName("Update Cart Item Quantity")
    class UpdateCartItemQuantity {

        @Test
        @DisplayName("Should update item quantity successfully")
        void updateCartItemQuantity_Success() {
            // Arrange
            testCart.getItems().add(testCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(testCartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartResponse response = cartService.updateCartItemQuantity(userEmail, productId, 5);

            // Assert
            assertThat(response).isNotNull();
            verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void updateCartItemQuantity_ProductNotFound_ThrowsException() {
            // Arrange
            UUID nonExistentProductId = UUID.randomUUID();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.updateCartItemQuantity(userEmail, nonExistentProductId, 5))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }
    }

    @Nested
    @DisplayName("Remove From Cart")
    class RemoveFromCart {

        @Test
        @DisplayName("Should remove item from cart successfully")
        void removeFromCart_Success() {
            // Arrange
            testCart.getItems().add(testCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(testCartItem));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartResponse response = cartService.removeFromCart(userEmail, productId);

            // Assert
            assertThat(response).isNotNull();
            verify(cartItemRepository).delete(testCartItem);
        }
    }

    @Nested
    @DisplayName("Clear Cart")
    class ClearCart {

        @Test
        @DisplayName("Should clear all items from cart")
        void clearCart_Success() {
            // Arrange
            testCart.getItems().add(testCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartResponse response = cartService.clearCart(userEmail);

            // Assert
            assertThat(response).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }
}
