package com.example.shopping.points.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.points.dto.PointAdjustRequest;
import com.example.shopping.points.dto.PointBalanceResponse;
import com.example.shopping.points.dto.PointTransactionResponse;
import com.example.shopping.points.service.PointService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members/{id}/points")
public class AdminPointController {

    private final PointService pointService;

    public AdminPointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<PointTransactionResponse>> transactions(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(pointService.listTransactions(id, pageable)));
    }

    @AdminAudit(action = "調整會員購物金", target = AuditTarget.MEMBER,
            detail = "#request.amount + ' 點:' + #request.reason")
    @PostMapping
    public ApiResponse<PointBalanceResponse> adjust(@PathVariable Long id,
                                                    @Valid @RequestBody PointAdjustRequest request) {
        return ApiResponse.success("購物金已調整", pointService.adjust(id, request.getAmount(), request.getReason()));
    }
}
