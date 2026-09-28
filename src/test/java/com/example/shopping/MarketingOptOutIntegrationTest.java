package com.example.shopping;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.reminder.CartReminderService;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.coupon.dto.request.CouponIssueRequest;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.repository.MemberCouponRepository;
import com.example.shopping.coupon.service.CouponIssueService;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.service.MemberService;
import com.example.shopping.notification.repository.NotificationRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 關閉行銷通知的會員:不收購物車提醒與發券通知,但券照樣放進錢包;可從個人資料切換 */
@SpringBootTest
@ActiveProfiles("test")
class MarketingOptOutIntegrationTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CartReminderService cartReminderService;
    @Autowired
    private CouponIssueService couponIssueService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    private long notificationsOf(Member member) {
        return notificationRepository.findByMemberId(member.getId(), PageRequest.of(0, 20)).getTotalElements();
    }

    @Test
    void optedOutMemberGetsNoMarketingNotifications() {
        Member member = new Member();
        member.setEmail("optout-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName("不要行銷");
        memberRepository.save(member);

        MemberUpdateRequest update = new MemberUpdateRequest();
        update.setName("不要行銷");
        update.setMarketingOptIn(false);
        assertThat(memberService.updateProfile(member.getId(), update).isMarketingOptIn()).isFalse();

        // 閒置購物車:不提醒
        Category category = new Category();
        category.setName("行銷偏好-" + suffix);
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("OPT-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(5);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);
        CartItem item = new CartItem();
        item.setMember(member);
        item.setProductSku(product.getSkus().get(0));
        item.setQuantity(1);
        cartItemRepository.save(item);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        jdbcTemplate.update("UPDATE cart_item SET updated_at = ? WHERE id = ?", now.minusHours(30), item.getId());
        cartReminderService.sendReminders(now);

        // 發券:券照樣進錢包,但不通知
        Coupon coupon = new Coupon();
        coupon.setCode("OPT" + suffix.toUpperCase());
        coupon.setName("測試券");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(new BigDecimal("50"));
        couponRepository.save(coupon);
        CouponIssueRequest issue = new CouponIssueRequest();
        issue.setTarget(CouponIssueRequest.Target.EMAILS);
        issue.setEmails(List.of(member.getEmail()));
        couponIssueService.issue(coupon.getId(), issue);

        assertThat(memberCouponRepository.existsByMemberIdAndCouponId(member.getId(), coupon.getId())).isTrue();
        assertThat(notificationsOf(member)).isZero();

        // 重新開啟後會收到提醒(購物車狀態沒被「認領」掉)
        update.setMarketingOptIn(true);
        memberService.updateProfile(member.getId(), update);
        cartReminderService.sendReminders(now);
        assertThat(notificationsOf(member)).isEqualTo(1);
    }
}
