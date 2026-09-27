package com.example.shopping.points.service;

import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.points.dto.PointBalanceResponse;
import com.example.shopping.points.dto.PointTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointService {

    PointBalanceResponse getBalance(Long memberId);

    Page<PointTransactionResponse> listTransactions(Long memberId, Pageable pageable);

    /** 扣除購物金,餘額不足時丟 BusinessException(整筆交易回滾) */
    void deduct(Long memberId, Long orderId, int amount, PointTransactionType type, String description);

    /** 增加購物金 */
    void credit(Long memberId, Long orderId, int amount, PointTransactionType type, String description);
}
