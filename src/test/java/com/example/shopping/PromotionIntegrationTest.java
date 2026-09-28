package com.example.shopping;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.promotion.AdminPromotionService;
import com.example.shopping.promotion.PromotionCalculator;
import com.example.shopping.promotion.PromotionRequest;
import com.example.shopping.promotion.PromotionResponse;
import com.example.shopping.promotion.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 父分類的活動套用到子分類商品;停用 / 未開始的活動不套用 */
@SpringBootTest
@ActiveProfiles("test")
class PromotionIntegrationTest {

    @Autowired
    private PromotionService promotionService;
    @Autowired
    private AdminPromotionService adminPromotionService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    private Category category(String name, Category parent) {
        Category category = new Category();
        category.setName(name + "-" + suffix);
        category.setParent(parent);
        return categoryRepository.save(category);
    }

    private ProductSku sku(Category category, String code, String price) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(code);
        product.setPrice(new BigDecimal(price));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode(code + "-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal(price));
        sku.setStock(10);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);
        return product.getSkus().get(0);
    }

    private CartItem cart(Member member, ProductSku sku, int quantity) {
        CartItem item = new CartItem();
        item.setMember(member);
        item.setProductSku(sku);
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    private PromotionRequest request(String name, Long categoryId, int minQty, int percent) {
        PromotionRequest request = new PromotionRequest();
        request.setName(name);
        request.setCategoryId(categoryId);
        request.setMinQuantity(minQty);
        request.setDiscountPercent(percent);
        return request;
    }

    @Test
    void parentCategoryPromotionCoversChildCategoryProducts() {
        Category women = category("女裝", null);
        Category tops = category("上衣", women);
        Category gadgets = category("3C", null);
        Member member = new Member();
        member.setEmail("promo-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName("買家");
        memberRepository.save(member);

        CartItem shirt = cart(member, sku(tops, "SHIRT", "500"), 1);
        CartItem blouse = cart(member, sku(tops, "BLOUSE", "300"), 1);
        CartItem phone = cart(member, sku(gadgets, "PHONE", "9000"), 1);

        PromotionResponse women2 = adminPromotionService.create(request("女裝 2 件 8 折 " + suffix, women.getId(), 2, 20));
        PromotionRequest future = request("未開始的活動 " + suffix, null, 2, 50);
        future.setStartAt(LocalDateTime.now().plusDays(1));
        adminPromotionService.create(future);
        PromotionResponse disabled = adminPromotionService.create(request("停用的活動 " + suffix, null, 2, 60));
        adminPromotionService.setActive(disabled.id(), false);

        PromotionCalculator.Result result = promotionService.previewForMember(member.getId(),
                List.of(shirt.getId(), blouse.getId(), phone.getId()));

        assertThat(result.promotionId()).isEqualTo(women2.id());
        assertThat(result.discount()).isEqualByComparingTo("160");

        // 只結帳一件上衣:未達門檻,提示再買 1 件
        PromotionCalculator.Result single = promotionService.previewForMember(member.getId(), List.of(shirt.getId()));
        assertThat(single.applied()).isFalse();
        assertThat(single.hints()).extracting(PromotionCalculator.Hint::promotionId).contains(women2.id());
    }
}
