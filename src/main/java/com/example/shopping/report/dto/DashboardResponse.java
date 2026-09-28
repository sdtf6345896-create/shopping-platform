package com.example.shopping.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/** 後台首頁總覽:今日營運數字 + 待處理事項數量 */
@Getter
@AllArgsConstructor
public class DashboardResponse {

    private long todayOrders;
    private BigDecimal todayRevenue;
    private long todayNewMembers;

    /** 待付款訂單 */
    private long pendingPaymentOrders;
    /** 已付款、等待出貨的訂單 */
    private long ordersToShip;
    /** 待審核的退貨申請 */
    private long pendingReturns;
    /** 尚未回覆的商品提問 */
    private long unansweredQuestions;
    /** 最後一則是會員留言、等待回覆的訂單 */
    private long awaitingOrderMessages;
    /** 低庫存(小於等於門檻)的上架商品規格數 */
    private long lowStockSkus;
    private int lowStockThreshold;
}
