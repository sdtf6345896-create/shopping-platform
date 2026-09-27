package com.example.shopping.product.event;

/**
 * 商品即將被刪除(在同一個交易內同步發布)。
 * 其他模組監聽後清掉自己引用該商品的資料;若不允許刪除(例如已有訂單)可直接丟例外,整筆交易會回滾。
 */
public record ProductDeletingEvent(Long productId, String productName) {
}
