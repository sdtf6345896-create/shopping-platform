package com.example.shopping.wishlist.event;

import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品刪除或會員刪除帳號時,清掉相關資料 */
@Component
public class WishlistCleanupListener {

    private final WishlistItemRepository repository;

    public WishlistCleanupListener(WishlistItemRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        repository.deleteAllByProductId(event.productId());
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        repository.deleteAllByMemberId(event.memberId());
    }
}
