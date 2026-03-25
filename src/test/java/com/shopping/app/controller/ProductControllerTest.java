package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.ProductRequest;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.ProductResponse;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.ProductService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.shopping.app.support.SecurityTestHelper.roleJwt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@SecuredControllerTest
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ProductResponse testProductResponse;
    private ProductRequest testProductRequest;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        testProductResponse = ProductResponse.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("A test product")
                .shortDescription("Short desc")
                .sku("TEST-001")
                .price(new BigDecimal("29.99"))
                .compareAtPrice(new BigDecimal("39.99"))
                .currency("EUR")
                .stockQuantity(100)
                .inStock(true)
                .lowStock(false)
                .onSale(true)
                .brand("TestBrand")
                .imageUrl("https://example.com/image.jpg")
                .categoryName("Electronics")
                .categoryId(1L)
                .ratingAverage(new BigDecimal("4.50"))
                .ratingCount(10)
                .active(true)
                .featured(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProductRequest = ProductRequest.builder()
                .name("Test Product")
                .description("A test product")
                .shortDescription("Short desc")
                .sku("TEST-001")
                .price(new BigDecimal("29.99"))
                .compareAtPrice(new BigDecimal("39.99"))
                .stockQuantity(100)
                .brand("TestBrand")
                .imageUrl("https://example.com/image.jpg")
                .categoryId(1L)
                .featured(false)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetAllProducts {

        @Test
        @DisplayName("Should return 200 with paginated products")
        void getAllProducts_Returns200() throws Exception {
            PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                    .content(List.of(testProductResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(productService.getAllProducts(any(Pageable.class))).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/products")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].name", is("Test Product")))
                    .andExpect(jsonPath("$.data.totalElements", is(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("Should return 200 with product when found")
        void getProductById_Returns200() throws Exception {
            when(productService.getProductById(productId)).thenReturn(testProductResponse);

            mockMvc.perform(get("/api/v1/products/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id", is(productId.toString())))
                    .andExpect(jsonPath("$.data.name", is("Test Product")))
                    .andExpect(jsonPath("$.data.price", is(29.99)));
        }

        @Test
        @DisplayName("Should return 404 when product not found")
        void getProductById_Returns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(productService.getProductById(nonExistentId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", nonExistentId));

            mockMvc.perform(get("/api/v1/products/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @DisplayName("Should return 201 when seller creates product")
        void createProduct_Returns201() throws Exception {
            when(productService.createProduct(any(ProductRequest.class), anyString()))
                    .thenReturn(testProductResponse);

            mockMvc.perform(post("/api/v1/products")
                            .with(roleJwt("seller@test.com", "ROLE_SELLER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testProductRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name", is("Test Product")))
                    .andExpect(jsonPath("$.data.sku", is("TEST-001")));
        }

        @Test
        @DisplayName("Should return 400 with invalid input")
        void createProduct_InvalidInput_Returns400() throws Exception {
            ProductRequest invalidRequest = ProductRequest.builder()
                    .name("")
                    .sku("")
                    .price(null)
                    .build();

            mockMvc.perform(post("/api/v1/products")
                            .with(roleJwt("seller@test.com", "ROLE_SELLER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("Should return 200 when product updated")
        void updateProduct_Returns200() throws Exception {
            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(productId)
                    .name("Updated Product")
                    .slug("updated-product")
                    .sku("TEST-001")
                    .price(new BigDecimal("39.99"))
                    .stockQuantity(200)
                    .active(true)
                    .build();

            when(productService.updateProduct(eq(productId), any(ProductRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/products/{id}", productId)
                            .with(roleJwt("seller@test.com", "ROLE_SELLER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testProductRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name", is("Updated Product")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("Should return 200 when admin deletes product")
        void deleteProduct_Returns200() throws Exception {
            mockMvc.perform(delete("/api/v1/products/{id}", productId)
                            .with(roleJwt("admin@test.com", "ROLE_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/featured")
    class GetFeaturedProducts {

        @Test
        @DisplayName("Should return 200 with featured products")
        void getFeaturedProducts_Returns200() throws Exception {
            ProductResponse featuredProduct = ProductResponse.builder()
                    .id(UUID.randomUUID())
                    .name("Featured Product")
                    .slug("featured-product")
                    .sku("FEAT-001")
                    .price(new BigDecimal("49.99"))
                    .featured(true)
                    .active(true)
                    .build();

            PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                    .content(List.of(featuredProduct))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(productService.getFeaturedProducts(any(Pageable.class))).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/products/featured"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].featured", is(true)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products?search=keyword")
    class SearchProducts {

        @Test
        @DisplayName("Should return 200 with search results")
        void searchProducts_Returns200() throws Exception {
            PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                    .content(List.of(testProductResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(productService.searchProducts(eq("Test"), any(Pageable.class))).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/products")
                            .param("search", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].name", is("Test Product")));
        }
    }
}
