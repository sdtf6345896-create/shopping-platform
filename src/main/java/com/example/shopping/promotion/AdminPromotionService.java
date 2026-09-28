package com.example.shopping.promotion;

import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AdminPromotionService {

    private final PromotionRepository promotionRepository;
    private final CategoryRepository categoryRepository;

    public AdminPromotionService(PromotionRepository promotionRepository, CategoryRepository categoryRepository) {
        this.promotionRepository = promotionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> list() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAllForAdmin().stream().map(p -> PromotionResponse.from(p, now)).toList();
    }

    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = new Promotion();
        apply(promotion, request);
        return PromotionResponse.from(promotionRepository.save(promotion), LocalDateTime.now());
    }

    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = findOrThrow(id);
        apply(promotion, request);
        return PromotionResponse.from(promotion, LocalDateTime.now());
    }

    public PromotionResponse setActive(Long id, boolean active) {
        Promotion promotion = findOrThrow(id);
        promotion.setActive(active);
        return PromotionResponse.from(promotion, LocalDateTime.now());
    }

    /** 訂單只存活動名稱與金額快照,刪除活動不影響歷史訂單 */
    public void delete(Long id) {
        promotionRepository.delete(findOrThrow(id));
    }

    private void apply(Promotion promotion, PromotionRequest request) {
        if (request.getStartAt() != null && request.getEndAt() != null
                && !request.getEndAt().isAfter(request.getStartAt())) {
            throw new BusinessException("活動結束時間必須晚於開始時間");
        }
        promotion.setName(request.getName().trim());
        promotion.setCategory(request.getCategoryId() == null ? null
                : categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new BusinessException("分類不存在")));
        promotion.setMinQuantity(request.getMinQuantity());
        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setStartAt(request.getStartAt());
        promotion.setEndAt(request.getEndAt());
        promotion.setActive(request.isActive());
    }

    private Promotion findOrThrow(Long id) {
        return promotionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("活動不存在"));
    }
}
