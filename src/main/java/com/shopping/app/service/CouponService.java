package com.shopping.app.service;

import com.shopping.app.dto.request.ApplyCouponRequest;
import com.shopping.app.dto.response.CouponResponse;
import com.shopping.app.entity.Cart;
import com.shopping.app.entity.Coupon;
import com.shopping.app.entity.User;
import com.shopping.app.exception.BadRequestException;
import com.shopping.app.exception.ResourceNotFoundException;
import com.shopping.app.repository.CartRepository;
import com.shopping.app.repository.CouponRepository;
import com.shopping.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CouponResponse applyCoupon(String userEmail, ApplyCouponRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", request.getCode()));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon is expired or no longer valid");
        }

        BigDecimal cartTotal = cart.getTotalAmount();

        if (cartTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    String.format("Minimum order amount of %s EUR required to use this coupon", coupon.getMinOrderAmount()));
        }

        BigDecimal discountAmount = coupon.calculateDiscount(cartTotal);
        BigDecimal finalTotal = cartTotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .discountAmount(discountAmount)
                .cartTotal(cartTotal)
                .finalTotal(finalTotal)
                .build();
    }

    @Transactional(readOnly = true)
    public CouponResponse validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon is expired or no longer valid");
        }

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .build();
    }
}
