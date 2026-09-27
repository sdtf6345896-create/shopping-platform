package com.example.shopping.question.event;

import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.question.repository.ProductQuestionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品刪除時一併清掉引用它的資料 */
@Component
public class QuestionProductCleaner {

    private final ProductQuestionRepository repository;

    public QuestionProductCleaner(ProductQuestionRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        repository.deleteAllByProductId(event.productId());
    }
}
