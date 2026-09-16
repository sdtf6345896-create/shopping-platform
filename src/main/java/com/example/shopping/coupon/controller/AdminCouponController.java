package com.example.shopping.coupon.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.coupon.dto.request.CouponRequest;
import com.example.shopping.coupon.dto.request.CouponStatusRequest;
import com.example.shopping.coupon.dto.response.CouponResponse;
import com.example.shopping.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CouponResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CouponStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ApiResponse.success(PageResponse.from(couponService.listAdmin(keyword, status, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CouponResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(couponService.getAdmin(id));
    }

    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("新增成功", couponService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("更新成功", couponService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CouponResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody CouponStatusRequest request) {
        return ApiResponse.success("狀態更新成功", couponService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
