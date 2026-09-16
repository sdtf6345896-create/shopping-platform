package com.example.shopping.coupon.service;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon fixedCoupon;
    private Coupon percentageCoupon;

    @BeforeEach
    void setUp() {
        fixedCoupon = new Coupon();
        fixedCoupon.setId(1L);
        fixedCoupon.setCode("SAVE100");
        fixedCoupon.setName("折抵 100 元");
        fixedCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        fixedCoupon.setDiscountValue(new BigDecimal("100"));
        fixedCoupon.setMinSpendAmount(new BigDecimal("500"));
        fixedCoupon.setStatus(CouponStatus.ACTIVE);

        percentageCoupon = new Coupon();
        percentageCoupon.setId(2L);
        percentageCoupon.setCode("SAVE10PCT");
        percentageCoupon.setName("9折,上限 300 元");
        percentageCoupon.setDiscountType(DiscountType.PERCENTAGE);
        percentageCoupon.setDiscountValue(new BigDecimal("10"));
        percentageCoupon.setMaxDiscountAmount(new BigDecimal("300"));
        percentageCoupon.setMinSpendAmount(BigDecimal.ZERO);
        percentageCoupon.setStatus(CouponStatus.ACTIVE);
    }

    @Test
    void preview_calculatesFixedDiscount_andDoesNotConsumeQuota() {
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(fixedCoupon));

        CouponApplyResponse response = couponService.preview("SAVE100", new BigDecimal("1000"));

        assertThat(response.getDiscountAmount()).isEqualByComparingTo("100");
        assertThat(response.getPayableAmount()).isEqualByComparingTo("900");
        assertThat(fixedCoupon.getUsedQuantity()).isZero();
    }

    @Test
    void preview_capsPercentageDiscount_atMaxDiscountAmount() {
        when(couponRepository.findByCodeIgnoreCase("SAVE10PCT")).thenReturn(Optional.of(percentageCoupon));

        CouponApplyResponse response = couponService.preview("SAVE10PCT", new BigDecimal("5000"));

        // 10% of 5000 = 500, capped at 300
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("300");
        assertThat(response.getPayableAmount()).isEqualByComparingTo("4700");
    }

    @Test
    void preview_throws_whenBelowMinSpend() {
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(fixedCoupon));

        assertThatThrownBy(() -> couponService.preview("SAVE100", new BigDecimal("100")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("門檻");
    }

    @Test
    void preview_throws_whenCouponNotFound() {
        when(couponRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.preview("NOPE", BigDecimal.TEN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void preview_throws_whenCouponDisabled() {
        fixedCoupon.setStatus(CouponStatus.DISABLED);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(fixedCoupon));

        assertThatThrownBy(() -> couponService.preview("SAVE100", new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用");
    }

    @Test
    void preview_throws_whenExpired() {
        fixedCoupon.setEndAt(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(fixedCoupon));

        assertThatThrownBy(() -> couponService.preview("SAVE100", new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("過期");
    }

    @Test
    void preview_throws_whenQuotaExhausted() {
        fixedCoupon.setTotalQuantity(1);
        fixedCoupon.setUsedQuantity(1);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(fixedCoupon));

        assertThatThrownBy(() -> couponService.preview("SAVE100", new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("兌換完畢");
    }

    @Test
    void reserve_incrementsUsedQuantity() {
        fixedCoupon.setTotalQuantity(10);
        when(couponRepository.findByCodeIgnoreCase("SAVE100")).thenReturn(Optional.of(fixedCoupon));

        couponService.reserve("SAVE100", new BigDecimal("1000"));

        assertThat(fixedCoupon.getUsedQuantity()).isEqualTo(1);
    }

    @Test
    void release_decrementsUsedQuantity() {
        fixedCoupon.setUsedQuantity(3);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(fixedCoupon));

        couponService.release(1L);

        assertThat(fixedCoupon.getUsedQuantity()).isEqualTo(2);
    }

    @Test
    void release_doesNotGoBelowZero() {
        fixedCoupon.setUsedQuantity(0);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(fixedCoupon));

        couponService.release(1L);

        assertThat(fixedCoupon.getUsedQuantity()).isZero();
    }

    @Test
    void delete_throws_whenCouponAlreadyUsed() {
        fixedCoupon.setUsedQuantity(2);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(fixedCoupon));

        assertThatThrownBy(() -> couponService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無法刪除");
    }
}
