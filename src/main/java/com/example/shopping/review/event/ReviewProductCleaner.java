package com.example.shopping.review.event;

import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.review.repository.ProductReviewRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品刪除時一併清掉引用它的資料 */
@Component
public class ReviewProductCleaner {

    private final ProductReviewRepository repository;

    public ReviewProductCleaner(ProductReviewRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        repository.deleteAllByProductId(event.productId());
    }
}
