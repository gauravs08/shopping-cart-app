package com.shopping.app.controller;

import com.shopping.app.dto.request.ApplyCouponRequest;
import com.shopping.app.dto.response.ApiResponse;
import com.shopping.app.dto.response.CouponResponse;
import com.shopping.app.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon and promo code endpoints")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/apply")
    @Operation(summary = "Apply a coupon to the current cart")
    public ResponseEntity<ApiResponse<CouponResponse>> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            Principal principal) {
        String email = principal.getName();
        CouponResponse response = couponService.applyCoupon(email, request);
        return ResponseEntity.ok(ApiResponse.success("Coupon applied successfully", response));
    }

    @GetMapping("/validate/{code}")
    @Operation(summary = "Validate a coupon code")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(@PathVariable String code) {
        CouponResponse response = couponService.validateCoupon(code);
        return ResponseEntity.ok(ApiResponse.success("Coupon is valid", response));
    }
}
