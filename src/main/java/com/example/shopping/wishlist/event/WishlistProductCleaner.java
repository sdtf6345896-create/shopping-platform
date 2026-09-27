package com.example.shopping.wishlist.event;

import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品刪除時一併清掉引用它的資料 */
@Component
public class WishlistProductCleaner {

    private final WishlistItemRepository repository;

    public WishlistProductCleaner(WishlistItemRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        repository.deleteAllByProductId(event.productId());
    }
}
