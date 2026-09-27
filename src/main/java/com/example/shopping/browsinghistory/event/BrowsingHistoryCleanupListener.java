package com.example.shopping.browsinghistory.event;

import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.product.event.ProductDeletingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 商品刪除或會員刪除帳號時,清掉相關資料 */
@Component
public class BrowsingHistoryCleanupListener {

    private final BrowsingHistoryItemRepository repository;

    public BrowsingHistoryCleanupListener(BrowsingHistoryItemRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        repository.deleteAllByProductId(event.productId());
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        repository.deleteByMemberId(event.memberId());
    }
}
