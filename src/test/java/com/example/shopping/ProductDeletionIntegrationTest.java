package com.example.shopping;

import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
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
import com.example.shopping.product.service.ProductService;
import com.example.shopping.question.entity.ProductQuestion;
import com.example.shopping.question.repository.ProductQuestionRepository;
import com.example.shopping.wishlist.entity.WishlistItem;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 沒有訂單的商品可以刪除,購物車、收藏、瀏覽紀錄、提問等引用它的資料會一併清掉,不會撞外鍵。
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductDeletionIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private WishlistItemRepository wishlistItemRepository;
    @Autowired
    private BrowsingHistoryItemRepository browsingHistoryItemRepository;
    @Autowired
    private ProductQuestionRepository questionRepository;

    @Test
    void deletingUnsoldProduct_cleansUpEverythingReferencingIt() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Member member = new Member();
        member.setEmail("del-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName("刪除測試");
        member.setEmailVerified(true);
        memberRepository.save(member);

        Category category = new Category();
        category.setName("刪除測試-" + suffix);
        categoryRepository.save(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("即將刪除的商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("DEL-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(5);
        product.replaceSkus(List.of(sku));
        product.replaceImages(List.of("https://img/1.jpg"));
        productRepository.save(product);

        CartItem cartItem = new CartItem();
        cartItem.setMember(member);
        cartItem.setProductSku(product.getSkus().get(0));
        cartItem.setQuantity(1);
        cartItemRepository.save(cartItem);

        WishlistItem wish = new WishlistItem();
        wish.setMember(member);
        wish.setProduct(product);
        wishlistItemRepository.save(wish);

        BrowsingHistoryItem viewed = new BrowsingHistoryItem();
        viewed.setMember(member);
        viewed.setProduct(product);
        viewed.setViewedAt(LocalDateTime.now());
        browsingHistoryItemRepository.save(viewed);

        ProductQuestion question = new ProductQuestion();
        question.setMember(member);
        question.setProduct(product);
        question.setContent("還有貨嗎?");
        questionRepository.save(question);

        productService.delete(product.getId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
        assertThat(cartItemRepository.findById(cartItem.getId())).isEmpty();
        assertThat(wishlistItemRepository.findById(wish.getId())).isEmpty();
        assertThat(browsingHistoryItemRepository.findById(viewed.getId())).isEmpty();
        assertThat(questionRepository.findById(question.getId())).isEmpty();
    }
}
