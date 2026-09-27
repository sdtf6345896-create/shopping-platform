package com.example.shopping.browsinghistory.event;

import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
import com.example.shopping.product.event.ProductDeletingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品刪除時一併清掉引用它的資料 */
@Component
public class BrowsingHistoryProductCleaner {

    private final BrowsingHistoryItemRepository repository;

    public BrowsingHistoryProductCleaner(BrowsingHistoryItemRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        repository.deleteAllByProductId(event.productId());
    }
}
