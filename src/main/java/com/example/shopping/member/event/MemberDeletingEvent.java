package com.example.shopping.member.event;

/**
 * 會員即將刪除帳號(同一個交易內同步發布)。各模組清掉自己保存的該會員資料;
 * 若不允許刪除(例如還有處理中的訂單)可直接丟例外,整筆交易回滾。
 */
public record MemberDeletingEvent(Long memberId) {
}
