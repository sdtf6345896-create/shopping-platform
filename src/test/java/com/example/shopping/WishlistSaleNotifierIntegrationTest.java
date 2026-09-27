package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.repository.NotificationRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.wishlist.entity.WishlistItem;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import com.example.shopping.wishlist.service.WishlistSaleNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 收藏的商品開特價 → 通知一次;同一檔不重複通知;換一檔新特價再通知 */
@SpringBootTest
@ActiveProfiles("test")
class WishlistSaleNotifierIntegrationTest {

    @Autowired
    private WishlistSaleNotifier notifier;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WishlistItemRepository wishlistItemRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private long notificationsOf(Member member) {
        return notificationRepository.findByMemberId(member.getId(), PageRequest.of(0, 50)).getTotalElements();
    }

    @Test
    void notifiesOncePerSale() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = new Member();
        member.setEmail("wish-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName("收藏家");
        member.setEmailVerified(true);
        memberRepository.save(member);

        Category category = new Category();
        category.setName("收藏測試-" + suffix);
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("心願單商品");
        product.setPrice(new BigDecimal("1000"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("WISH-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("1000"));
        sku.setStock(5);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);

        WishlistItem wish = new WishlistItem();
        wish.setMember(member);
        wish.setProduct(product);
        wishlistItemRepository.save(wish);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        notifier.notifySales(now);
        assertThat(notificationsOf(member)).as("沒有特價時不通知").isZero();

        product.setSaleDiscountPercent(20);
        product.setSaleStartAt(now.minusMinutes(1));
        product.setSaleEndAt(now.plusDays(1));
        productRepository.save(product);

        notifier.notifySales(now);
        notifier.notifySales(now);
        assertThat(notificationsOf(member)).as("同一檔特價只通知一次").isEqualTo(1);

        // 換一檔新的特價(開始時間不同)→ 再通知一次
        product.setSaleStartAt(now.minusSeconds(10));
        productRepository.save(product);
        notifier.notifySales(now);
        assertThat(notificationsOf(member)).isEqualTo(2);
    }
}
