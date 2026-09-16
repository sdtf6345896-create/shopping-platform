package com.example.shopping.coupon.service;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.coupon.dto.request.CouponRequest;
import com.example.shopping.coupon.dto.request.CouponStatusRequest;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.dto.response.CouponResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.example.shopping.coupon.repository.CouponSpecifications.hasStatus;
import static com.example.shopping.coupon.repository.CouponSpecifications.keywordMatches;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> listAdmin(String keyword, CouponStatus status, Pageable pageable) {
        Specification<Coupon> spec = Specification.where(keywordMatches(keyword)).and(hasStatus(status));
        return couponRepository.findAll(spec, pageable).map(CouponResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getAdmin(Long id) {
        return CouponResponse.from(findOrThrow(id));
    }

    @Override
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BusinessException("優惠券代碼已存在");
        }
        Coupon coupon = new Coupon();
        applyRequest(coupon, request);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findOrThrow(id);
        couponRepository.findByCodeIgnoreCase(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("優惠券代碼已存在");
            }
        });
        applyRequest(coupon, request);
        return CouponResponse.from(coupon);
    }

    @Override
    public CouponResponse updateStatus(Long id, CouponStatusRequest request) {
        Coupon coupon = findOrThrow(id);
        coupon.setStatus(request.getStatus());
        return CouponResponse.from(coupon);
    }

    @Override
    public void delete(Long id) {
        Coupon coupon = findOrThrow(id);
        if (coupon.getUsedQuantity() > 0) {
            throw new BusinessException("此優惠券已有人使用,無法刪除,請改為停用");
        }
        couponRepository.delete(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponApplyResponse preview(String code, BigDecimal subtotal) {
        Coupon coupon = validateCoupon(code, subtotal);
        return toApplyResponse(coupon, subtotal);
    }

    @Override
    public CouponApplyResponse reserve(String code, BigDecimal subtotal) {
        Coupon coupon = validateCoupon(code, subtotal);
        coupon.setUsedQuantity(coupon.getUsedQuantity() + 1);
        return toApplyResponse(coupon, subtotal);
    }

    @Override
    public void release(Long couponId) {
        couponRepository.findById(couponId).ifPresent(coupon -> {
            if (coupon.getUsedQuantity() > 0) {
                coupon.setUsedQuantity(coupon.getUsedQuantity() - 1);
            }
        });
    }

    private Coupon validateCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("請輸入優惠券代碼");
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("優惠券代碼不存在"));

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BusinessException("此優惠券已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartAt() != null && now.isBefore(coupon.getStartAt())) {
            throw new BusinessException("此優惠券尚未開始使用");
        }
        if (coupon.getEndAt() != null && now.isAfter(coupon.getEndAt())) {
            throw new BusinessException("此優惠券已過期");
        }
        if (coupon.getTotalQuantity() != null && coupon.getUsedQuantity() >= coupon.getTotalQuantity()) {
            throw new BusinessException("此優惠券已被兌換完畢");
        }
        if (subtotal.compareTo(coupon.getMinSpendAmount()) < 0) {
            throw new BusinessException("訂單金額未達 NT$ " + coupon.getMinSpendAmount() + " 門檻,無法使用此優惠券");
        }
        return coupon;
    }

    private CouponApplyResponse toApplyResponse(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = coupon.calculateDiscount(subtotal);
        return new CouponApplyResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                discount,
                subtotal.subtract(discount));
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        if (request.getStartAt() != null && request.getEndAt() != null
                && request.getStartAt().isAfter(request.getEndAt())) {
            throw new BusinessException("開始時間不可晚於結束時間");
        }
        coupon.setCode(request.getCode().trim().toUpperCase());
        coupon.setName(request.getName());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMinSpendAmount(request.getMinSpendAmount());
        coupon.setTotalQuantity(request.getTotalQuantity());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
    }

    private Coupon findOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("優惠券不存在"));
    }
}
