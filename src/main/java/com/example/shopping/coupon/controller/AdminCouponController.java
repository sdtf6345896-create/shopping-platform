package com.example.shopping.coupon.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.coupon.dto.request.CouponIssueRequest;
import com.example.shopping.coupon.dto.request.CouponRequest;
import com.example.shopping.coupon.dto.request.CouponStatusRequest;
import com.example.shopping.coupon.dto.response.CouponIssueResponse;
import com.example.shopping.coupon.dto.response.CouponResponse;
import com.example.shopping.coupon.service.CouponIssueService;
import com.example.shopping.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;
    private final CouponIssueService couponIssueService;

    public AdminCouponController(CouponService couponService, CouponIssueService couponIssueService) {
        this.couponService = couponService;
        this.couponIssueService = couponIssueService;
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

    @AdminAudit(action = "新增優惠券", target = AuditTarget.COUPON, detail = "#request.code")
    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("新增成功", couponService.create(request));
    }

    @AdminAudit(action = "修改優惠券", target = AuditTarget.COUPON, detail = "#request.code")
    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("更新成功", couponService.update(id, request));
    }

    @AdminAudit(action = "優惠券啟用/停用", target = AuditTarget.COUPON, detail = "#request.status")
    @PatchMapping("/{id}/status")
    public ApiResponse<CouponResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody CouponStatusRequest request) {
        return ApiResponse.success("狀態更新成功", couponService.updateStatus(id, request));
    }

    @AdminAudit(action = "發放優惠券", target = AuditTarget.COUPON,
            detail = "#request.target + (#request.minTier != null ? ' ' + #request.minTier : '')"
                    + " + ' → 新發 ' + #result.data.issued() + ' 人'")
    @PostMapping("/{id}/issue")
    public ApiResponse<CouponIssueResponse> issue(@PathVariable Long id, @Valid @RequestBody CouponIssueRequest request) {
        CouponIssueResponse result = couponIssueService.issue(id, request);
        return ApiResponse.success("已發放給 " + result.issued() + " 位會員", result);
    }

    @AdminAudit(action = "刪除優惠券", target = AuditTarget.COUPON)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
