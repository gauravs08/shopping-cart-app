package com.shopping.app.service;

import com.shopping.app.dto.request.ReviewRequest;
import com.shopping.app.dto.response.PagedResponse;
import com.shopping.app.dto.response.ReviewResponse;
import com.shopping.app.entity.Product;
import com.shopping.app.entity.Review;
import com.shopping.app.entity.User;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.OrderItemRepository;
import com.shopping.app.repository.ProductRepository;
import com.shopping.app.repository.ReviewRepository;
import com.shopping.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public ReviewResponse createReview(UUID productId, String userEmail, ReviewRequest request) {
        User user = findUserByEmail(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        boolean verifiedPurchase = orderItemRepository
                .existsByUserIdAndProductIdAndOrderDelivered(user.getId(), productId);

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .verifiedPurchase(verifiedPurchase)
                .build();

        review = reviewRepository.save(review);

        updateProductRating(product);

        return mapToResponse(review);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviewsByProduct(UUID productId, Pageable pageable) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Page<Review> page = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return mapToPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviewsByUser(String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        Page<Review> page = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return mapToPagedResponse(page);
    }

    @Transactional
    public void deleteReview(UUID reviewId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);

        updateProductRating(product);
    }

    private void updateProductRating(Product product) {
        BigDecimal average = reviewRepository.calculateAverageRating(product.getId());
        int count = reviewRepository.countByProductId(product.getId());
        product.setRatingAverage(average);
        product.setRatingCount(count);
        productRepository.save(product);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private PagedResponse<ReviewResponse> mapToPagedResponse(Page<Review> page) {
        return PagedResponse.<ReviewResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
