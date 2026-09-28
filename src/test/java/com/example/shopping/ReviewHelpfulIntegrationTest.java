package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.helpful.ReviewHelpfulService;
import com.example.shopping.review.helpful.ReviewHelpfulVoteRepository;
import com.example.shopping.review.repository.ProductReviewRepository;
import com.example.shopping.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 有幫助投票:每人一票、可收回、不能投自己;「最有幫助」排序;評價刪除時投票一併刪除 */
@SpringBootTest
@ActiveProfiles("test")
class ReviewHelpfulIntegrationTest {

    @Autowired
    private ReviewHelpfulService helpfulService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ProductReviewRepository reviewRepository;
    @Autowired
    private ReviewHelpfulVoteRepository voteRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    private Member member(String name) {
        Member member = new Member();
        member.setEmail(name + "-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName(name);
        return memberRepository.save(member);
    }

    private ProductReview review(Product product, Member author, String content) {
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setMember(author);
        review.setRating(5);
        review.setContent(content);
        return reviewRepository.save(review);
    }

    @Test
    void votingFlow() {
        Category category = new Category();
        category.setName("有幫助-" + suffix);
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("投票商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        productRepository.save(product);

        Member alice = member("alice");
        Member bob = member("bob");
        Member carol = member("carol");
        ProductReview older = review(product, alice, "詳細的使用心得");
        ProductReview newer = review(product, bob, "還不錯");
        // 兩則在同一秒內建立,時間相同時「新到舊」的順序不確定,把舊的那則往前調
        jdbcTemplate.update("UPDATE product_review SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(1), older.getId());

        helpfulService.vote(bob.getId(), product.getId(), older.getId());
        helpfulService.vote(bob.getId(), product.getId(), older.getId());   // 重複按不重複計
        assertThat(helpfulService.vote(carol.getId(), product.getId(), older.getId()).helpfulCount()).isEqualTo(2);
        assertThatThrownBy(() -> helpfulService.vote(alice.getId(), product.getId(), older.getId()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> helpfulService.vote(bob.getId(), -1L, older.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // 最有幫助:票多的在前;預設仍是新到舊
        assertThat(reviewService.listByProduct(product.getId(), false, true, PageRequest.of(0, 10)).getContent())
                .extracting(ReviewResponse::getId).containsExactly(older.getId(), newer.getId());
        assertThat(reviewService.listByProduct(product.getId(), false, false, PageRequest.of(0, 10)).getContent())
                .extracting(ReviewResponse::getId).containsExactly(newer.getId(), older.getId());
        assertThat(helpfulService.myVotes(bob.getId(), product.getId())).containsExactly(older.getId());

        assertThat(helpfulService.unvote(bob.getId(), product.getId(), older.getId()).helpfulCount()).isEqualTo(1);
        assertThat(helpfulService.unvote(bob.getId(), product.getId(), older.getId()).helpfulCount()).isEqualTo(1);

        // 評論者刪除自己的評價:投票一併刪除,不會卡外鍵
        reviewService.delete(alice.getId(), product.getId());
        assertThat(voteRepository.existsByReviewIdAndMemberId(older.getId(), carol.getId())).isFalse();
    }
}
