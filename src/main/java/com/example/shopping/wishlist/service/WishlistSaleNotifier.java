package com.example.shopping.wishlist.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.wishlist.entity.WishlistItem;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 收藏的商品開始限時特價時,以站內通知提醒收藏者(同一檔特價只通知一次) */
@Service
@Transactional
public class WishlistSaleNotifier {

    private final WishlistItemRepository wishlistItemRepository;
    private final NotificationService notificationService;

    public WishlistSaleNotifier(WishlistItemRepository wishlistItemRepository,
                                NotificationService notificationService) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.notificationService = notificationService;
    }

    /** @return 發出的通知數 */
    public int notifySales(LocalDateTime now) {
        List<WishlistItem> items = wishlistItemRepository.findUnnotifiedOnSale(ProductStatus.ON_SALE, now);
        for (WishlistItem item : items) {
            Product product = item.getProduct();
            notificationService.notify(item.getMember().getId(), NotificationType.SYSTEM,
                    "收藏商品限時特價 -" + product.getSaleDiscountPercent() + "%",
                    "你收藏的「" + product.getName() + "」正在特價,特價價 NT$ "
                            + product.applySale(product.getPrice()).stripTrailingZeros().toPlainString() + " 起,把握機會!",
                    "/products/" + product.getId());
            item.setSaleNotifiedStart(product.getSaleStartAt());
        }
        return items.size();
    }
}
