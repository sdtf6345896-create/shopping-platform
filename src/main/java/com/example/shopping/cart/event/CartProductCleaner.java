package com.example.shopping.cart.event;

import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品或規格被刪除時,從所有會員的購物車移除 */
@Component
public class CartProductCleaner {

    private final CartItemRepository cartItemRepository;

    public CartProductCleaner(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        cartItemRepository.deleteByProductId(event.productId());
    }

    @EventListener
    public void onSkusRemoving(SkusRemovingEvent event) {
        cartItemRepository.deleteBySkuIds(event.skuIds());
    }
}
