package com.example.shopping.returns.service;

import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.returns.dto.request.ReturnApplyRequest;
import com.example.shopping.returns.dto.request.ReturnDecisionRequest;
import com.example.shopping.returns.dto.response.ReturnRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnService {

    /** 會員申請退貨:訂單須為已完成且在鑑賞期內,每筆訂單只能申請一次 */
    OrderResponse apply(Long memberId, Long orderId, ReturnApplyRequest request);

    Page<ReturnRequestResponse> listAdmin(ReturnStatus status, Pageable pageable);

    /** 核准:訂單改為 REFUNDED、商品回庫存、退還使用的購物金、收回回饋的購物金 */
    ReturnRequestResponse approve(Long returnId, ReturnDecisionRequest request);

    ReturnRequestResponse reject(Long returnId, ReturnDecisionRequest request);
}
