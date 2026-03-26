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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest request, String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", sellerEmail));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        String slug = generateSlug(request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .costPrice(request.getCostPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold() > 0 ? request.getLowStockThreshold() : 5)
                .weight(request.getWeight())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .category(category)
                .seller(seller)
                .featured(request.isFeatured())
                .build();

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        product.setName(request.getName());
        product.setSlug(generateSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCostPrice(request.getCostPrice());
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }
        product.setStockQuantity(request.getStockQuantity());
        if (request.getLowStockThreshold() > 0) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }
        product.setWeight(request.getWeight());
        product.setBrand(request.getBrand());
        product.setImageUrl(request.getImageUrl());
        product.setFeatured(request.isFeatured());

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByActiveTrue(pageable);
        return mapToPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        Page<Product> page = productRepository.findByCategoryId(categoryId, pageable);
        return mapToPagedResponse(page);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "featuredProducts")
    public PagedResponse<ProductResponse> getFeaturedProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByFeaturedTrueAndActiveTrue(pageable);
        return mapToPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String query, Pageable pageable) {
        Page<Product> page = productRepository.searchByNameOrDescription(query, pageable);
        return mapToPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> filterProducts(String keyword, String brand,
            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating, Pageable pageable) {
        Page<Product> page = productRepository.findByFilters(keyword, brand, minPrice, maxPrice, minRating, pageable);
        return mapToPagedResponse(page);
    }

    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setActive(false);
        productRepository.save(product);
    }

    private String generateSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-" + "$", "");
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return baseSlug + "-" + uniqueSuffix;
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .currency(product.getCurrency())
                .stockQuantity(product.getStockQuantity())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .active(product.isActive())
                .featured(product.isFeatured())
                .inStock(product.isInStock())
                .lowStock(product.isLowStock())
                .onSale(product.isOnSale())
                .ratingAverage(product.getRatingAverage())
                .ratingCount(product.getRatingCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private PagedResponse<ProductResponse> mapToPagedResponse(Page<Product> page) {
        return PagedResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
