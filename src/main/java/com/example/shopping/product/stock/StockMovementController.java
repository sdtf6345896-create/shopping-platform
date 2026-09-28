package com.example.shopping.product.stock;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class StockMovementController {

    private final StockMovementRepository movementRepository;
    private final ProductSkuRepository productSkuRepository;

    public StockMovementController(StockMovementRepository movementRepository,
                                   ProductSkuRepository productSkuRepository) {
        this.movementRepository = movementRepository;
        this.productSkuRepository = productSkuRepository;
    }

    public record StockMovementResponse(Long id, int changeQty, int stockAfter, StockReason reason, String reference,
                                        LocalDateTime createdAt) {

        static StockMovementResponse from(StockMovement movement) {
            return new StockMovementResponse(movement.getId(), movement.getChangeQty(), movement.getStockAfter(),
                    movement.getReason(), movement.getReference(), movement.getCreatedAt());
        }
    }

    /** 後台:某規格的庫存異動紀錄,新到舊 */
    @GetMapping("/api/admin/products/{productId}/skus/{skuId}/stock-movements")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<StockMovementResponse>> list(@PathVariable Long productId,
                                                                 @PathVariable Long skuId,
                                                                 @PageableDefault(size = 20) Pageable pageable) {
        if (productSkuRepository.findByIdAndProductId(skuId, productId).isEmpty()) {
            throw new ResourceNotFoundException("規格不存在");
        }
        return ApiResponse.success(PageResponse.from(
                movementRepository.findBySkuIdOrderByIdDesc(skuId, pageable).map(StockMovementResponse::from)));
    }
}
