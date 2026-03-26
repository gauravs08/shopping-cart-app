package com.shopping.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.app.dto.request.ApplyCouponRequest;
import com.shopping.app.dto.response.CouponResponse;
import com.shopping.app.security.CustomUserDetailsService;
import com.shopping.app.service.CouponService;
import com.shopping.app.support.SecuredControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static com.shopping.app.support.SecurityTestHelper.userJwt;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@SecuredControllerTest
@DisplayName("CouponController Tests")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CouponResponse testCouponResponse;

    @BeforeEach
    void setUp() {
        testCouponResponse = CouponResponse.builder()
                .id(UUID.randomUUID())
                .code("SAVE10")
                .description("10% off")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.TEN)
                .minOrderAmount(BigDecimal.valueOf(50))
                .maxDiscountAmount(BigDecimal.valueOf(20))
                .discountAmount(BigDecimal.valueOf(15))
                .cartTotal(BigDecimal.valueOf(150))
                .finalTotal(BigDecimal.valueOf(135))
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/coupons/apply")
    class ApplyCoupon {

        @Test
        @DisplayName("Should return 200 when coupon applied")
        void applyCoupon_Returns200() throws Exception {
            when(couponService.applyCoupon(anyString(), any(ApplyCouponRequest.class)))
                    .thenReturn(testCouponResponse);

            String body = objectMapper.writeValueAsString(
                    ApplyCouponRequest.builder().code("SAVE10").build());

            mockMvc.perform(post("/api/v1/coupons/apply")
                            .with(userJwt("user@test.com"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.code", is("SAVE10")))
                    .andExpect(jsonPath("$.data.discountAmount").value(15));
        }

        @Test
        @DisplayName("Should return 401 without auth")
        void applyCoupon_NoAuth_Returns401() throws Exception {
            String body = objectMapper.writeValueAsString(
                    ApplyCouponRequest.builder().code("SAVE10").build());

            mockMvc.perform(post("/api/v1/coupons/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/coupons/validate/{code}")
    class ValidateCoupon {

        @Test
        @DisplayName("Should return 200 for valid coupon")
        void validateCoupon_Returns200() throws Exception {
            when(couponService.validateCoupon("SAVE10")).thenReturn(testCouponResponse);

            mockMvc.perform(get("/api/v1/coupons/validate/SAVE10")
                            .with(userJwt("user@test.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.code", is("SAVE10")));
        }
    }
}
