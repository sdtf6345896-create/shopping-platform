package com.example.shopping.stockalert.event;

import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import com.example.shopping.stockalert.repository.StockAlertRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品或規格刪除時清掉對應的貨到通知訂閱 */
@Component
public class StockAlertProductCleaner {

    private final StockAlertRepository stockAlertRepository;

    public StockAlertProductCleaner(StockAlertRepository stockAlertRepository) {
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
}
