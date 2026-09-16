package com.example.shopping.review.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.repository.OrderItemRepository;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.review.dto.request.ReviewRequest;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.dto.response.ReviewSummaryResponse;
import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.repository.ProductReviewRepository;
import com.example.shopping.review.repository.ReviewSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final Set<OrderStatus> PURCHASED_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public ReviewServiceImpl(ProductReviewRepository productReviewRepository,
                              OrderItemRepository orderItemRepository,
                              ProductRepository productRepository,
                              MemberRepository memberRepository) {
        this.productReviewRepository = productReviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByProduct(Long productId, Pageable pageable) {
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(ReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long productId) {
        ReviewSummaryProjection summary = productReviewRepository.summarizeByProductId(productId);
        double average = summary.getAverageRating() != null ? summary.getAverageRating() : 0d;
        long count = summary.getReviewCount() != null ? summary.getReviewCount() : 0L;
        return new ReviewSummaryResponse(Math.round(average * 10) / 10.0, count);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getMyReview(Long memberId, Long productId) {
        return productReviewRepository.findByProductIdAndMemberId(productId, memberId).map(ReviewResponse::from);
    }

    @Override
    public ReviewResponse upsert(Long memberId, Long productId, ReviewRequest request) {
        ProductReview review = productReviewRepository.findByProductIdAndMemberId(productId, memberId)
                .orElseGet(() -> createNewReview(memberId, productId));

        review.setRating(request.getRating());
        review.setContent(request.getContent());
        return ReviewResponse.from(productReviewRepository.save(review));
    }

    @Override
    public void delete(Long memberId, Long productId) {
        ProductReview review = productReviewRepository.findByProductIdAndMemberId(productId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("你尚未評論過此商品"));
        productReviewRepository.delete(review);
    }

    private ProductReview createNewReview(Long memberId, Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("商品不存在");
        }
        if (!orderItemRepository.existsPurchase(memberId, productId, PURCHASED_STATUSES)) {
            throw new BusinessException("需購買過此商品並完成付款,才能評論");
        }
        ProductReview review = new ProductReview();
        review.setProduct(productRepository.getReferenceById(productId));
        review.setMember(memberRepository.getReferenceById(memberId));
        return review;
    }
}
