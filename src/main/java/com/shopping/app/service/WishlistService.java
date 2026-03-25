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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<WishlistResponse> getWishlist(String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        Page<Wishlist> page = wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return PagedResponse.<WishlistResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public WishlistResponse addToWishlist(String userEmail, UUID productId) {
        User user = findUserByEmail(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BadRequestException("Product is already in your wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        wishlist = wishlistRepository.save(wishlist);
        return mapToResponse(wishlist);
    }

    @Transactional
    public void removeFromWishlist(String userEmail, UUID productId) {
        User user = findUserByEmail(userEmail);

        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist", "productId", productId));

        wishlistRepository.delete(wishlist);
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(String userEmail, UUID productId) {
        User user = findUserByEmail(userEmail);
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productImageUrl(product.getImageUrl())
                .productPrice(product.getPrice())
                .inStock(product.isInStock())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
