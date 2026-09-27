package com.example.shopping.cart.event;

import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品 / 規格被刪除時從所有購物車移除;會員刪除帳號時清空其購物車 */
@Component
public class CartCleanupListener {

    private final CartItemRepository cartItemRepository;

    public CartCleanupListener(CartItemRepository cartItemRepository) {
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

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        cartItemRepository.deleteByMemberId(event.memberId());
    }
}
