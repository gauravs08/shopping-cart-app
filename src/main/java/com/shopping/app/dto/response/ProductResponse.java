package com.shopping.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private String sku;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String currency;
    private int stockQuantity;
    private String brand;
    private String imageUrl;
    private String categoryName;
    private Long categoryId;
    private boolean active;
    private boolean featured;
    private boolean inStock;
    private boolean lowStock;
    private boolean onSale;
    private BigDecimal ratingAverage;
    private int ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
