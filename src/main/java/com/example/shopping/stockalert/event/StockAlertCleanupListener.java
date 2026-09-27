package com.example.shopping.stockalert.event;

import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import com.example.shopping.stockalert.repository.StockAlertRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品 / 規格刪除或會員刪除帳號時,清掉對應的貨到通知訂閱 */
@Component
public class StockAlertCleanupListener {

    private final StockAlertRepository stockAlertRepository;

    public StockAlertCleanupListener(StockAlertRepository stockAlertRepository) {
        this.stockAlertRepository = stockAlertRepository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        stockAlertRepository.deleteByProductId(event.productId());
    }

    @EventListener
    public void onSkusRemoving(SkusRemovingEvent event) {
        stockAlertRepository.deleteBySkuIds(event.skuIds());
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        stockAlertRepository.deleteAllByMemberId(event.memberId());
    }
}
