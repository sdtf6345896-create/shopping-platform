package com.example.shopping.product.stock;

import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品或規格刪除時,一併清掉它們的庫存異動紀錄 */
@Component
public class StockMovementCleanupListener {

    private final StockMovementRepository movementRepository;

    public StockMovementCleanupListener(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        movementRepository.deleteByProductId(event.productId());
    }

    @EventListener
    public void onSkusRemoving(SkusRemovingEvent event) {
        movementRepository.deleteBySkuIds(event.skuIds());
    }
}
