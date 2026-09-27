package com.example.shopping.order.dto.request;

import com.example.shopping.common.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 後台訂單查詢條件(列表與匯出共用),全部選填 */
@Getter
@Setter
public class AdminOrderQuery {

    private OrderStatus status;

    /** 只看某位會員的訂單(會員詳情頁用) */
    private Long memberId;

    /** 比對訂單編號、收件人、收件電話、會員 Email */
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
