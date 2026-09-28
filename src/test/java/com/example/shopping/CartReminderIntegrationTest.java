package com.example.shopping;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.reminder.CartReminderService;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
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

/** 閒置 24 小時的購物車提醒一次;再次異動後閒置才會再提醒;未閒置或停用帳號不提醒 */
@SpringBootTest
@ActiveProfiles("test")
class CartReminderIntegrationTest {

    @Autowired
    private CartReminderService reminderService;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    // 一律用真實的「現在」並把資料往回調,其他測試剛建立的購物車才不會被當成閒置而收到提醒
    private final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    private Member member(String name, AccountStatus status) {
        Member member = new Member();
        member.setEmail(name + "-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName(name);
        member.setStatus(status);
        return memberRepository.save(member);
    }

    private CartItem cartItem(Member member, ProductSku sku, LocalDateTime updatedAt) {
        CartItem item = new CartItem();
        item.setMember(member);
        item.setProductSku(sku);
        item.setQuantity(1);
        cartItemRepository.save(item);
        touch(item, updatedAt);
        return item;
    }

    private void touch(CartItem item, LocalDateTime updatedAt) {
        jdbcTemplate.update("UPDATE cart_item SET updated_at = ? WHERE id = ?", updatedAt, item.getId());
    }

    private long notificationsOf(Member member) {
        return notificationRepository.findByMemberId(member.getId(), PageRequest.of(0, 20)).getTotalElements();
    }

    @Test
    void remindsOncePerIdleCart() {
        Category category = new Category();
        category.setName("購物車提醒-" + suffix);
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("提醒商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("REMIND-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(10);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);
        ProductSku savedSku = product.getSkus().get(0);

        Member idle = member("idle", AccountStatus.ACTIVE);
        Member fresh = member("fresh", AccountStatus.ACTIVE);
        Member disabled = member("disabled", AccountStatus.DISABLED);
        CartItem idleItem = cartItem(idle, savedSku, now.minusHours(30));
        cartItem(fresh, savedSku, now.minusHours(2));
        cartItem(disabled, savedSku, now.minusHours(30));

        reminderService.sendReminders(now);
        assertThat(notificationsOf(idle)).isEqualTo(1);
        assertThat(notificationsOf(fresh)).isZero();
        assertThat(notificationsOf(disabled)).isZero();

        // 再跑一次:購物車沒變,不重複提醒
        reminderService.sendReminders(now);
        assertThat(notificationsOf(idle)).isEqualTo(1);

        // 模擬時間經過:上次提醒在 28 小時前,之後會員改過購物車(25 小時前)又放著 → 再提醒一次
        jdbcTemplate.update("UPDATE member SET cart_reminded_at = ? WHERE id = ?", now.minusHours(28), idle.getId());
        touch(idleItem, now.minusHours(25));
        reminderService.sendReminders(now);
        assertThat(notificationsOf(idle)).isEqualTo(2);
    }
}
