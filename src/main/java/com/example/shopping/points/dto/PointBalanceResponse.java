package com.example.shopping.points.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/** 購物金餘額與規則,前台結帳頁據此計算可折抵上限 */
@Getter
@AllArgsConstructor
public class PointBalanceResponse {

    private int balance;
    /** 訂單完成回饋比例(0.01 = 1%) */
    private BigDecimal earnRate;
    /** 單筆訂單最多可折抵應付金額的比例(0.5 = 50%) */
    private BigDecimal maxRedeemRatio;
}
