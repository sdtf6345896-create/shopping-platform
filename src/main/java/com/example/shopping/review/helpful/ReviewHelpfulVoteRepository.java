package com.example.shopping.review.helpful;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    boolean existsByReviewIdAndMemberId(Long reviewId, Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ReviewHelpfulVote v where v.review.id = :reviewId and v.memberId = :memberId")
    int deleteByReviewIdAndMemberId(@Param("reviewId") Long reviewId, @Param("memberId") Long memberId);

    /** 會員在某商品上按過「有幫助」的評價 id(前台顯示按鈕狀態用) */
    @Query("select v.review.id from ReviewHelpfulVote v where v.memberId = :memberId and v.review.product.id = :productId")
    List<Long> findVotedReviewIds(@Param("memberId") Long memberId, @Param("productId") Long productId);
}
