package com.example.shopping.product.event;

import java.util.List;

/**
 * 修改商品時有規格(SKU)被移除(在同一個交易內同步發布),語意同 {@link ProductDeletingEvent}。
 *
 * @param skus 被移除的規格(id 與顯示用名稱)
 */
public record SkusRemovingEvent(List<RemovedSku> skus) {

    public record RemovedSku(Long id, String label) {
    }

    public List<Long> skuIds() {
        return skus.stream().map(RemovedSku::id).toList();
    }
}
