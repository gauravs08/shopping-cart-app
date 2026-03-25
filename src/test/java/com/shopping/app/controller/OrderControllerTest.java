package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.OrderRequest;
import com.shopping.app.dto.response.OrderItemResponse;
import com.shopping.app.dto.response.OrderResponse;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import com.shopping.app.config.SecurityConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIGRldmVsb3BtZW50IG9ubHk=",
        "app.cors.allowed-origins=http://localhost:3000"
})
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private OrderResponse testOrderResponse;
    private OrderRequest testOrderRequest;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        OrderItemResponse orderItem = OrderItemResponse.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productName("Test Product")
                .productSku("TEST-001")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .totalPrice(new BigDecimal("100.00"))
                .build();

        testOrderResponse = OrderResponse.builder()
                .id(orderId)
                .orderNumber("ORD-20260324-0001")
                .status("PENDING")
                .items(List.of(orderItem))
                .subtotal(new BigDecimal("100.00"))
                .shippingCost(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.00"))
                .currency("EUR")
                .paymentMethod("CARD")
                .paymentStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testOrderRequest = OrderRequest.builder()
                .shippingAddressId(addressId)
                .paymentMethod("CARD")
                .notes("Test order")
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrder {

        @Test
        @DisplayName("Should return 201 when order is created")
        void createOrder_Returns201() throws Exception {
            // Arrange
            when(orderService.createOrder(anyString(), any(OrderRequest.class)))
                    .thenReturn(testOrderResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/orders")
                            .with(jwt().jwt(j -> j.subject("user@test.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testOrderRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.orderNumber", is("ORD-20260324-0001")))
                    .andExpect(jsonPath("$.data.status", is("PENDING")))
                    .andExpect(jsonPath("$.data.totalAmount", is(100.00)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetOrders {

        @Test
        @DisplayName("Should return 200 with paginated orders")
        void getOrders_Returns200() throws Exception {
            // Arrange
            PagedResponse<OrderResponse> pagedResponse = PagedResponse.<OrderResponse>builder()
                    .content(List.of(testOrderResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(orderService.getOrdersByUser(anyString(), any(Pageable.class)))
                    .thenReturn(pagedResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/orders")
                            .with(jwt().jwt(j -> j.subject("user@test.com")))
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalElements", is(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderById {

        @Test
        @DisplayName("Should return 200 with order details")
        void getOrderById_Returns200() throws Exception {
            // Arrange
            when(orderService.getOrderById(eq(orderId), anyString()))
                    .thenReturn(testOrderResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                            .with(jwt().jwt(j -> j.subject("user@test.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(orderId.toString())))
                    .andExpect(jsonPath("$.data.orderNumber", is("ORD-20260324-0001")))
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("Should return 200 when order is cancelled")
        void cancelOrder_Returns200() throws Exception {
            // Arrange
            OrderResponse cancelledOrder = OrderResponse.builder()
                    .id(orderId)
                    .orderNumber("ORD-20260324-0001")
                    .status("CANCELLED")
                    .items(testOrderResponse.getItems())
                    .subtotal(new BigDecimal("100.00"))
                    .totalAmount(new BigDecimal("100.00"))
                    .currency("EUR")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(orderService.cancelOrder(eq(orderId), anyString()))
                    .thenReturn(cancelledOrder);

            // Act & Assert
            mockMvc.perform(put("/api/v1/orders/{id}/cancel", orderId)
                            .with(jwt().jwt(j -> j.subject("user@test.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("CANCELLED")));
        }
    }
}
