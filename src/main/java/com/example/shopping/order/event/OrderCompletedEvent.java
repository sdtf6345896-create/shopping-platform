package com.example.shopping.order.event;

import java.math.BigDecimal;

/**
 * 訂單完成(會員確認收貨、管理員標記或系統自動完成)時發布,在同一個交易內同步處理。
 *
 * @param merchandiseAmount 實付金額扣掉運費
 */
public record OrderCompletedEvent(Long memberId, Long orderId, String orderNo, BigDecimal merchandiseAmount) {
}
