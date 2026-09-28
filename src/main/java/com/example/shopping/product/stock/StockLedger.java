package com.example.shopping.product.stock;

import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 記錄庫存異動。一律在呼叫端的交易內執行:庫存變動回滾時紀錄也一起回滾。
 * 異動後的庫存直接查資料庫(JPQL 查詢前會先 flush 尚未寫入的變更),併發下也是該筆異動後的真實值。
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class StockLedger {

    static final int MAX_REFERENCE_LENGTH = 100;

    private final StockMovementRepository movementRepository;
    private final ProductSkuRepository productSkuRepository;

    public StockLedger(StockMovementRepository movementRepository, ProductSkuRepository productSkuRepository) {
        this.movementRepository = movementRepository;
        this.productSkuRepository = productSkuRepository;
    }

    /** 記錄一筆異動;change 為 0 時不記 */
    public void record(Long skuId, int change, StockReason reason, String reference) {
        if (change == 0) {
            return;
        }
        StockMovement movement = new StockMovement();
        movement.setSkuId(skuId);
        movement.setChangeQty(change);
        movement.setStockAfter(productSkuRepository.findStockById(skuId));
        movement.setReason(reason);
        movement.setReference(truncate(reference));
        movementRepository.save(movement);
    }

    private static String truncate(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String trimmed = reference.trim();
        return trimmed.length() <= MAX_REFERENCE_LENGTH ? trimmed : trimmed.substring(0, MAX_REFERENCE_LENGTH);
    }
}
