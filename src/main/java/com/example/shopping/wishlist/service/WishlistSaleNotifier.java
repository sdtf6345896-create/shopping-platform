package com.example.shopping.wishlist.service;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.notification.service.MarketingNotifier;
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
    private final MarketingNotifier marketingNotifier;

    public WishlistSaleNotifier(WishlistItemRepository wishlistItemRepository,
                                MarketingNotifier marketingNotifier) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.marketingNotifier = marketingNotifier;
    }

    /**
     * 會員關閉行銷通知時不發,但一樣標記為已處理,之後重新開啟也不會補發舊的特價。
     *
     * @return 發出的通知數
     */
    public int notifySales(LocalDateTime now) {
        List<WishlistItem> items = wishlistItemRepository.findUnnotifiedOnSale(ProductStatus.ON_SALE, now);
        int sent = 0;
        for (WishlistItem item : items) {
            Product product = item.getProduct();
            boolean delivered = marketingNotifier.notify(item.getMember().getId(),
                    "收藏商品限時特價 -" + product.getSaleDiscountPercent() + "%",
                    "你收藏的「" + product.getName() + "」正在特價,特價價 NT$ "
                            + product.applySale(product.getPrice()).stripTrailingZeros().toPlainString() + " 起,把握機會!",
                    "/products/" + product.getId());
            item.setSaleNotifiedStart(product.getSaleStartAt());
            if (delivered) {
                sent++;
            }
        }
        return sent;
    }
}
