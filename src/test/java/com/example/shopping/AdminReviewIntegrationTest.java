package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.repository.NotificationRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.review.dto.response.AdminReviewResponse;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.repository.ProductReviewRepository;
import com.example.shopping.review.service.AdminReviewService;
import com.example.shopping.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 隱藏的評價不出現在前台、不列入評分;後台可篩選、回覆(首次回覆通知評論者) */
@SpringBootTest
@ActiveProfiles("test")
class AdminReviewIntegrationTest {

    @Autowired
    private AdminReviewService adminReviewService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ProductReviewRepository reviewRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    private Member member(String name) {
        Member member = new Member();
        member.setEmail(name + "-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName(name);
        member.setEmailVerified(true);
        return memberRepository.save(member);
    }

    private ProductReview review(Product product, Member member, int rating, String content) {
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setMember(member);
        review.setRating(rating);
        review.setContent(content);
        return reviewRepository.save(review);
    }

    @Test
    void hideReplyAndFilter() {
        Category category = new Category();
        category.setName("評價測試");
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("評價管理測試商品-" + suffix);
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        productRepository.save(product);

        Member happy = member("開心");
        Member angry = member("生氣");
        ProductReview good = review(product, happy, 5, "很好用");
        ProductReview spam = review(product, angry, 1, "垃圾廣告 加我 LINE");
        reviewService.refreshProductRating(product.getId());
        assertThat(reviewService.getSummary(product.getId()).getAverageRating()).isEqualTo(3.0);

        adminReviewService.setHidden(spam.getId(), true);

        assertThat(reviewService.listByProduct(product.getId(), false, PageRequest.of(0, 10)).getContent())
                .extracting(ReviewResponse::getId).containsExactly(good.getId());
        assertThat(reviewService.getSummary(product.getId()).getReviewCount()).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getRatingAverage())
                .isEqualByComparingTo("5.0");
        // 評論者本人仍看得到自己的評價,並知道被隱藏了
        assertThat(reviewService.getMyReview(angry.getId(), product.getId()).orElseThrow().isHidden()).isTrue();

        adminReviewService.reply(good.getId(), "  謝謝支持!  ");
        adminReviewService.reply(good.getId(), "謝謝支持,歡迎再次光臨!");
        assertThat(notificationRepository.findByMemberId(happy.getId(), PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(reviewService.listByProduct(product.getId(), false, PageRequest.of(0, 10)).getContent().get(0)
                .getSellerReply()).isEqualTo("謝謝支持,歡迎再次光臨!");

        String keyword = "評價管理測試商品-" + suffix;
        assertThat(adminReviewService.search(null, null, null, keyword, PageRequest.of(0, 10)).getContent())
                .hasSize(2);
        assertThat(adminReviewService.search(null, false, null, keyword, PageRequest.of(0, 10)).getContent())
                .extracting(AdminReviewResponse::id).containsExactly(spam.getId());
        assertThat(adminReviewService.search(1, null, true, keyword, PageRequest.of(0, 10)).getContent())
                .extracting(AdminReviewResponse::memberName).containsExactly("生氣");
        assertThat(adminReviewService.search(5, null, true, keyword, PageRequest.of(0, 10)).getContent()).isEmpty();

        // 清掉回覆
        assertThat(adminReviewService.reply(good.getId(), "   ").sellerReply()).isNull();
    }
}
