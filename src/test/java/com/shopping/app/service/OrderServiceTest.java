package com.shopping.app.service;

import com.shopping.app.dto.request.OrderRequest;
import com.shopping.app.dto.response.OrderResponse;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.entity.Address;
import com.shopping.app.entity.Cart;
import com.shopping.app.entity.CartItem;
import com.shopping.app.entity.Order;
import com.shopping.app.entity.OrderItem;
import com.shopping.app.entity.OrderStatus;
import com.shopping.app.entity.Product;
import com.shopping.app.entity.User;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.InsufficientStockException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.AddressRepository;
import com.shopping.app.repository.CartRepository;
import com.shopping.app.repository.OrderRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;
    private Order testOrder;
    private Address testAddress;
    private OrderRequest testOrderRequest;
    private UUID userId;
    private UUID orderId;
    private UUID addressId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        userEmail = "buyer@test.com";

        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .firstName("Test")
                .lastName("Buyer")
                .password("encoded-password")
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("Order Product")
                .slug("order-product")
                .sku("ORD-001")
                .price(new BigDecimal("50.00"))
                .stockQuantity(20)
                .active(true)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testAddress = Address.builder()
                .id(addressId)
                .user(testUser)
                .label("Home")
                .street("123 Test Street")
                .city("Helsinki")
                .state("Uusimaa")
                .postalCode("00100")
                .country("Finland")
                .isDefault(true)
                .build();

        testCart = Cart.builder()
                .id(UUID.randomUUID())
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

        testOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-20260324-0001")
                .user(testUser)
                .status(OrderStatus.PENDING)
                .shippingAddress(testAddress)
                .subtotal(new BigDecimal("100.00"))
                .shippingCost(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.00"))
                .currency("EUR")
                .paymentMethod("CARD")
                .paymentStatus("PENDING")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(testOrder)
                .product(testProduct)
                .productName("Order Product")
                .productSku("ORD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        testOrder.getItems().add(orderItem);

        testOrderRequest = OrderRequest.builder()
                .shippingAddressId(addressId)
                .paymentMethod("CARD")
                .notes("Please deliver before 5 PM")
                .build();
    }

    @Nested
    @DisplayName("Create Order")
    class CreateOrder {

        @Test
        @DisplayName("Should create order from cart successfully")
        void createOrder_Success_CreatesFromCart() {
            // Arrange
            testCart.getItems().add(testCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(testAddress));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            OrderResponse response = orderService.createOrder(userEmail, testOrderRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).isNotNull();
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            verify(orderRepository).save(any(Order.class));
            verify(productRepository).save(any(Product.class));
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should throw exception when cart is empty")
        void createOrder_EmptyCart_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(userEmail, testOrderRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when product has insufficient stock")
        void createOrder_InsufficientStock_ThrowsException() {
            // Arrange
            Product lowStockProduct = Product.builder()
                    .id(UUID.randomUUID())
                    .name("Low Stock")
                    .slug("low-stock")
                    .sku("LOW-001")
                    .price(new BigDecimal("10.00"))
                    .stockQuantity(1)
                    .active(true)
                    .ratingAverage(BigDecimal.ZERO)
                    .ratingCount(0)
                    .build();

            CartItem lowStockCartItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .cart(testCart)
                    .product(lowStockProduct)
                    .quantity(5)
                    .createdAt(LocalDateTime.now())
                    .build();

            testCart.getItems().add(lowStockCartItem);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(testAddress));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(userEmail, testOrderRequest))
                    .isInstanceOf(InsufficientStockException.class);

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Get Order By ID")
    class GetOrderById {

        @Test
        @DisplayName("Should return order when found and belongs to user")
        void getOrderById_Success() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // Act
            OrderResponse response = orderService.getOrderById(orderId, userEmail);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(orderId);
            assertThat(response.getOrderNumber()).isEqualTo("ORD-20260324-0001");
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should throw exception when order belongs to different user")
        void getOrderById_WrongUser_ThrowsException() {
            // Arrange
            String otherEmail = "other@test.com";
            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(otherEmail)
                    .build();
            when(userRepository.findByEmail(otherEmail)).thenReturn(Optional.of(otherUser));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrderById(orderId, otherEmail))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Get Orders By User")
    class GetOrdersByUser {

        @Test
        @DisplayName("Should return paginated orders for user")
        void getOrdersByUser_ReturnsPaginatedResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(orderRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(orderPage);

            // Act
            PagedResponse<OrderResponse> response = orderService.getOrdersByUser(userEmail, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            verify(orderRepository).findByUserOrderByCreatedAtDesc(testUser, pageable);
        }
    }

    @Nested
    @DisplayName("Cancel Order")
    class CancelOrder {

        @Test
        @DisplayName("Should cancel pending order and restore stock")
        void cancelOrder_PendingOrder_Success() {
            // Arrange
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            OrderResponse response = orderService.cancelOrder(orderId, userEmail);

            // Assert
            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when cancelling shipped order")
        void cancelOrder_ShippedOrder_ThrowsException() {
            // Arrange
            testOrder.setStatus(OrderStatus.SHIPPED);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, userEmail))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cancel");
        }
    }

    @Nested
    @DisplayName("Update Order Status")
    class UpdateOrderStatus {

        @Test
        @DisplayName("Should update order status with valid transition")
        void updateOrderStatus_ValidTransition_Success() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

            // Assert
            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }
    }
}
