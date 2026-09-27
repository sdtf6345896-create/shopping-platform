package com.example.shopping.recommendation.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 唯讀查詢:從訂單明細統計「一起被購買」的商品 */
public interface CoPurchaseRepository extends Repository<OrderItem, Long> {

    /**
     * 和指定商品出現在同一筆有效訂單中的其他上架商品,依共同出現的訂單數排序。
     */
    @Query("select other.productSku.product.id as productId, count(distinct item.order.id) as orderCount "
            + "from OrderItem item join OrderItem other on other.order = item.order "
            + "where item.productSku.product.id = :productId "
            + "and other.productSku.product.id <> :productId "
            + "and other.productSku.product.status = :productStatus "
            + "and item.order.status in :orderStatuses "
            + "group by other.productSku.product.id "
            + "order by count(distinct item.order.id) desc, other.productSku.product.id asc")
    List<CoPurchaseProjection> findBoughtTogether(@Param("productId") Long productId,
                                                  @Param("productStatus") ProductStatus productStatus,
                                                  @Param("orderStatuses") Collection<OrderStatus> orderStatuses,
                                                  Pageable pageable);
}
