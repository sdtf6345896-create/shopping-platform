package com.example.shopping.promotion;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.product.entity.ProductSku;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 滿件折扣:依購物車 / 結帳商品挑出最划算的進行中活動 */
@Service
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CartItemRepository cartItemRepository;

    public PromotionService(PromotionRepository promotionRepository, CartItemRepository cartItemRepository) {
        this.promotionRepository = promotionRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public List<Promotion> listRunning(LocalDateTime now) {
        return promotionRepository.findRunning(now);
    }

    /** 以購物車項目試算(結帳與購物車頁共用;價格用當下實際售價,含限時特價) */
    public PromotionCalculator.Result evaluate(List<CartItem> items, LocalDateTime now) {
        List<PromotionCalculator.Rule> rules = listRunning(now).stream()
                .map(p -> new PromotionCalculator.Rule(p.getId(), p.getName(),
                        p.getCategory() == null ? null : p.getCategory().getId(),
                        p.getMinQuantity(), p.getDiscountPercent()))
                .toList();
        if (rules.isEmpty() || items.isEmpty()) {
            return PromotionCalculator.Result.none();
        }
        List<PromotionCalculator.Line> lines = items.stream()
                .map(item -> {
                    ProductSku sku = item.getProductSku();
                    return new PromotionCalculator.Line(categoryPath(sku.getProduct().getCategory()),
                            sku.getEffectivePrice(), item.getQuantity());
                })
                .toList();
        return PromotionCalculator.evaluate(rules, lines);
    }

    /** 會員購物車試算;cartItemIds 為空表示整個購物車 */
    public PromotionCalculator.Result previewForMember(Long memberId, List<Long> cartItemIds) {
        List<CartItem> items = cartItemIds == null || cartItemIds.isEmpty()
                ? cartItemRepository.findAllByMemberIdWithDetails(memberId)
                : cartItemIds.stream()
                        .map(id -> cartItemRepository.findByIdAndMemberId(id, memberId)
                                .orElseThrow(() -> new ResourceNotFoundException("購物車項目不存在:" + id)))
                        .toList();
        return evaluate(items, LocalDateTime.now());
    }

    private static Set<Long> categoryPath(Category category) {
        Set<Long> path = new HashSet<>();
        // 上限防呆:資料若出現循環也不會卡死
        for (Category c = category; c != null && path.size() < 20; c = c.getParent()) {
            if (!path.add(c.getId())) {
                break;
            }
        }
        return path;
    }
}
