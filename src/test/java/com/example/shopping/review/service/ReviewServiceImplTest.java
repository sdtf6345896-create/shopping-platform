package com.example.shopping.review.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.repository.OrderItemRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.review.dto.request.ReviewRequest;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.dto.response.ReviewSummaryResponse;
import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.repository.ProductReviewRepository;
import com.example.shopping.review.repository.ReviewSummaryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Product product;
    private Member member;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(10L);

        member = new Member();
        member.setId(1L);
        member.setName("陳小美");
    }

    @Test
    void upsert_createsReview_whenMemberHasPurchased() {
        when(productReviewRepository.findByProductIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());
        when(productRepository.existsById(10L)).thenReturn(true);
        when(orderItemRepository.existsPurchase(eq(1L), eq(10L), any())).thenReturn(true);
        when(productRepository.getReferenceById(10L)).thenReturn(product);
        when(memberRepository.getReferenceById(1L)).thenReturn(member);
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setContent("很好用");

        ReviewResponse response = reviewService.upsert(1L, 10L, request);

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("很好用");
        assertThat(response.getMemberName()).isEqualTo("陳**");
    }

    @Test
    void upsert_throws_whenMemberHasNotPurchased() {
        when(productReviewRepository.findByProductIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());
        when(productRepository.existsById(10L)).thenReturn(true);
        when(orderItemRepository.existsPurchase(eq(1L), eq(10L), any())).thenReturn(false);

        ReviewRequest request = new ReviewRequest();
        request.setRating(5);

        assertThatThrownBy(() -> reviewService.upsert(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("需購買過此商品");
    }

    @Test
    void upsert_throws_whenProductNotFound() {
        when(productReviewRepository.findByProductIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());
        when(productRepository.existsById(10L)).thenReturn(false);

        ReviewRequest request = new ReviewRequest();
        request.setRating(4);

        assertThatThrownBy(() -> reviewService.upsert(1L, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void upsert_updatesExistingReview_withoutRecheckingPurchase() {
        ProductReview existing = new ProductReview();
        existing.setId(99L);
        existing.setProduct(product);
        existing.setMember(member);
        existing.setRating(3);
        existing.setContent("普通");
        when(productReviewRepository.findByProductIdAndMemberId(10L, 1L)).thenReturn(Optional.of(existing));
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setContent("改觀了,很棒");

        ReviewResponse response = reviewService.upsert(1L, 10L, request);

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("改觀了,很棒");
        org.mockito.Mockito.verifyNoInteractions(orderItemRepository);
    }

    @Test
    void delete_throws_whenNoExistingReview() {
        when(productReviewRepository.findByProductIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.delete(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSummary_roundsAverageToOneDecimal() {
        ReviewSummaryProjection projection = mockProjection(4.666, 3L);
        when(productReviewRepository.summarizeByProductId(10L)).thenReturn(projection);

        ReviewSummaryResponse summary = reviewService.getSummary(10L);

        assertThat(summary.getAverageRating()).isEqualTo(4.7);
        assertThat(summary.getReviewCount()).isEqualTo(3L);
    }

    @Test
    void getSummary_returnsZero_whenNoReviewsYet() {
        ReviewSummaryProjection projection = mockProjection(null, 0L);
        when(productReviewRepository.summarizeByProductId(10L)).thenReturn(projection);

        ReviewSummaryResponse summary = reviewService.getSummary(10L);

        assertThat(summary.getAverageRating()).isEqualTo(0d);
        assertThat(summary.getReviewCount()).isZero();
    }

    private ReviewSummaryProjection mockProjection(Double average, Long count) {
        return new ReviewSummaryProjection() {
            @Override
            public Double getAverageRating() {
                return average;
            }

            @Override
            public Long getReviewCount() {
                return count;
            }
        };
    }
}
