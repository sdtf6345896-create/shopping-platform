package com.example.shopping.review.helpful;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.repository.ProductReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 評價「有幫助」投票:每人每則一票、不能投自己的評價、被隱藏的評價不能投 */
@Service
@Transactional
public class ReviewHelpfulService {

    private final ProductReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository voteRepository;

    public ReviewHelpfulService(ProductReviewRepository reviewRepository, ReviewHelpfulVoteRepository voteRepository) {
        this.reviewRepository = reviewRepository;
        this.voteRepository = voteRepository;
    }

    public record VoteResponse(Long reviewId, int helpfulCount, boolean voted) {
    }

    public VoteResponse vote(Long memberId, Long productId, Long reviewId) {
        ProductReview review = findVisible(productId, reviewId);
        if (review.getMember().getId().equals(memberId)) {
            throw new BusinessException("不能對自己的評價按「有幫助」");
        }
        if (!voteRepository.existsByReviewIdAndMemberId(reviewId, memberId)) {
            ReviewHelpfulVote vote = new ReviewHelpfulVote();
            vote.setReview(review);
            vote.setMemberId(memberId);
            voteRepository.save(vote);
            reviewRepository.addHelpfulCount(reviewId, 1);
        }
        return new VoteResponse(reviewId, reviewRepository.findHelpfulCountById(reviewId), true);
    }

    public VoteResponse unvote(Long memberId, Long productId, Long reviewId) {
        findVisible(productId, reviewId);
        if (voteRepository.deleteByReviewIdAndMemberId(reviewId, memberId) > 0) {
            reviewRepository.addHelpfulCount(reviewId, -1);
        }
        return new VoteResponse(reviewId, reviewRepository.findHelpfulCountById(reviewId), false);
    }

    @Transactional(readOnly = true)
    public List<Long> myVotes(Long memberId, Long productId) {
        return voteRepository.findVotedReviewIds(memberId, productId);
    }

    private ProductReview findVisible(Long productId, Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .filter(r -> r.getProduct().getId().equals(productId) && !r.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("評價不存在"));
        return review;
    }
}
