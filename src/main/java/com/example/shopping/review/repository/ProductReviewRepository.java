package com.example.shopping.review.repository;

import com.example.shopping.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /** 前台列表:不含被隱藏的評價 */
    Page<ProductReview> findByProductIdAndHiddenFalseOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /** 前台列表依「有幫助」票數排序(同票數新的在前) */
    Page<ProductReview> findByProductIdAndHiddenFalseOrderByHelpfulCountDescCreatedAtDesc(Long productId,
                                                                                          Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("update ProductReview r set r.helpfulCount = r.helpfulCount + :delta where r.id = :reviewId")
    int addHelpfulCount(@Param("reviewId") Long reviewId, @Param("delta") int delta);

    @Query("select r.helpfulCount from ProductReview r where r.id = :reviewId")
    int findHelpfulCountById(@Param("reviewId") Long reviewId);

    Optional<ProductReview> findByProductIdAndMemberId(Long productId, Long memberId);

    @Query(value = "select r from ProductReview r where r.product.id = :productId and r.hidden = false "
            + "and r.images is not empty order by r.createdAt desc",
            countQuery = "select count(r) from ProductReview r where r.product.id = :productId "
                    + "and r.hidden = false and r.images is not empty")
    Page<ProductReview> findWithImagesByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("select avg(r.rating) as averageRating, count(r) as reviewCount " +
            "from ProductReview r where r.product.id = :productId and r.hidden = false")
    ReviewSummaryProjection summarizeByProductId(@Param("productId") Long productId);

    /** 後台評價管理:條件皆為選填;keyword 比對商品名稱 */
    @Query(value = "select r from ProductReview r join fetch r.product p join fetch r.member m"
            + " where (:rating is null or r.rating = :rating)"
            + " and (:replied is null or (:replied = true and r.sellerReply is not null)"
            + "      or (:replied = false and r.sellerReply is null))"
            + " and (:hidden is null or r.hidden = :hidden)"
            + " and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))",
            countQuery = "select count(r) from ProductReview r join r.product p"
                    + " where (:rating is null or r.rating = :rating)"
                    + " and (:replied is null or (:replied = true and r.sellerReply is not null)"
                    + "      or (:replied = false and r.sellerReply is null))"
                    + " and (:hidden is null or r.hidden = :hidden)"
                    + " and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))")
    Page<ProductReview> searchAdmin(@Param("rating") Integer rating, @Param("replied") Boolean replied,
                                    @Param("hidden") Boolean hidden, @Param("keyword") String keyword,
                                    Pageable pageable);

    @Modifying
    @Query("delete from ProductReview x where x.product.id = :productId")
    int deleteAllByProductId(@Param("productId") Long productId);
}
