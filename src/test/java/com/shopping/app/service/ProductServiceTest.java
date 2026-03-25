package com.shopping.app.service;

import com.shopping.app.dto.request.ProductRequest;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.ProductResponse;
import com.shopping.app.entity.Category;
import com.shopping.app.entity.Product;
import com.shopping.app.entity.User;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.CategoryRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;
    private Category testCategory;
    private User testSeller;
    private UUID productId;
    private String sellerEmail;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        sellerEmail = "seller@test.com";

        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .active(true)
                .build();

        testSeller = User.builder()
                .id(sellerId)
                .email(sellerEmail)
                .firstName("John")
                .lastName("Seller")
                .password("encoded-password")
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("A test product description")
                .shortDescription("Short desc")
                .sku("TEST-SKU-001")
                .price(new BigDecimal("29.99"))
                .compareAtPrice(new BigDecimal("39.99"))
                .stockQuantity(100)
                .brand("TestBrand")
                .imageUrl("https://example.com/image.jpg")
                .category(testCategory)
                .seller(testSeller)
                .active(true)
                .featured(false)
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProductRequest = ProductRequest.builder()
                .name("Test Product")
                .description("A test product description")
                .shortDescription("Short desc")
                .sku("TEST-SKU-001")
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
    @DisplayName("Create Product")
    class CreateProduct {

        @Test
        @DisplayName("Should create product successfully with slug generation")
        void createProduct_Success() {
            // Arrange
            when(userRepository.findByEmail(sellerEmail)).thenReturn(Optional.of(testSeller));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            ProductResponse response = productService.createProduct(testProductRequest, sellerEmail);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Test Product");
            assertThat(response.getSku()).isEqualTo("TEST-SKU-001");
            assertThat(response.getSlug()).isNotNull();
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when seller not found")
        void createProduct_SellerNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(sellerEmail)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(testProductRequest, sellerEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("Get Product By ID")
    class GetProductById {

        @Test
        @DisplayName("Should return product when found")
        void getProductById_Found() {
            // Arrange
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

            // Act
            ProductResponse response = productService.getProductById(productId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(productId);
            assertThat(response.getName()).isEqualTo("Test Product");
            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product not found")
        void getProductById_NotFound_ThrowsResourceNotFoundException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.getProductById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");

            verify(productRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Get All Products")
    class GetAllProducts {

        @Test
        @DisplayName("Should return paginated products")
        void getAllProducts_ReturnsPaginatedResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);
            when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

            // Act
            PagedResponse<ProductResponse> response = productService.getAllProducts(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getPage()).isZero();
            assertThat(response.getSize()).isEqualTo(10);
            verify(productRepository).findByActiveTrue(pageable);
        }
    }

    @Nested
    @DisplayName("Get Products By Category")
    class GetProductsByCategory {

        @Test
        @DisplayName("Should return paginated products for a category")
        void getProductsByCategory_ReturnsPaginatedResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.findByCategoryId(1L, pageable)).thenReturn(productPage);

            // Act
            PagedResponse<ProductResponse> response = productService.getProductsByCategory(1L, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getCategoryName()).isEqualTo("Electronics");
            verify(productRepository).findByCategoryId(1L, pageable);
        }
    }

    @Nested
    @DisplayName("Get Featured Products")
    class GetFeaturedProducts {

        @Test
        @DisplayName("Should return paginated featured products")
        void getFeaturedProducts_ReturnsPaginatedResults() {
            // Arrange
            Product featuredProduct = Product.builder()
                    .id(UUID.randomUUID())
                    .name("Featured Product")
                    .slug("featured-product")
                    .sku("FEAT-001")
                    .price(new BigDecimal("49.99"))
                    .stockQuantity(50)
                    .active(true)
                    .featured(true)
                    .ratingAverage(BigDecimal.ZERO)
                    .ratingCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> featuredPage = new PageImpl<>(List.of(featuredProduct), pageable, 1);
            when(productRepository.findByFeaturedTrueAndActiveTrue(pageable)).thenReturn(featuredPage);

            // Act
            PagedResponse<ProductResponse> response = productService.getFeaturedProducts(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isNotEmpty();
            assertThat(response.getContent().get(0).isFeatured()).isTrue();
            verify(productRepository).findByFeaturedTrueAndActiveTrue(pageable);
        }
    }

    @Nested
    @DisplayName("Search Products")
    class SearchProducts {

        @Test
        @DisplayName("Should return products matching search keyword")
        void searchProducts_ByName_ReturnsResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);
            when(productRepository.searchByNameOrDescription(eq("Test"), eq(pageable)))
                    .thenReturn(productPage);

            // Act
            PagedResponse<ProductResponse> response = productService.searchProducts("Test", pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getName()).contains("Test");
            verify(productRepository).searchByNameOrDescription(eq("Test"), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Update Product")
    class UpdateProduct {

        @Test
        @DisplayName("Should update product successfully")
        void updateProduct_Success() {
            // Arrange
            ProductRequest updateRequest = ProductRequest.builder()
                    .name("Updated Product")
                    .description("Updated description")
                    .sku("TEST-SKU-001")
                    .price(new BigDecimal("39.99"))
                    .stockQuantity(200)
                    .categoryId(1L)
                    .build();

            Product updatedProduct = Product.builder()
                    .id(productId)
                    .name("Updated Product")
                    .slug("updated-product")
                    .description("Updated description")
                    .sku("TEST-SKU-001")
                    .price(new BigDecimal("39.99"))
                    .stockQuantity(200)
                    .category(testCategory)
                    .active(true)
                    .ratingAverage(BigDecimal.ZERO)
                    .ratingCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

            // Act
            ProductResponse response = productService.updateProduct(productId, updateRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Updated Product");
            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("39.99"));
            verify(productRepository).findById(productId);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when product to update not found")
        void updateProduct_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(nonExistentId, testProductRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");

            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("Delete Product")
    class DeleteProduct {

        @Test
        @DisplayName("Should soft-delete product successfully")
        void deleteProduct_SoftDeletes() {
            // Arrange
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            productService.deleteProduct(productId);

            // Assert
            verify(productRepository).findById(productId);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent product")
        void deleteProduct_NotFound_ThrowsException() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");

            verify(productRepository, never()).save(any(Product.class));
        }
    }
}
