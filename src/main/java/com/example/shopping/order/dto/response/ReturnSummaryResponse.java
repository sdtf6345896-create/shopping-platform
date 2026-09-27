package com.example.shopping.order.dto.response;

import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.returns.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** 訂單詳情中附帶的退貨申請摘要 */
@Getter
@AllArgsConstructor
public class ReturnSummaryResponse {

    private Long id;
    private ReturnStatus status;
    private String reason;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static ReturnSummaryResponse from(ReturnRequest request) {
        return request == null ? null : new ReturnSummaryResponse(request.getId(), request.getStatus(),
                request.getReason(), request.getAdminNote(), request.getCreatedAt(), request.getProcessedAt());
    }
}
