package com.example.shopping.points.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.points.dto.PointBalanceResponse;
import com.example.shopping.points.dto.PointTransactionResponse;
import com.example.shopping.points.service.PointService;
import com.example.shopping.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping
    public ApiResponse<PointBalanceResponse> balance() {
        return ApiResponse.success(pointService.getBalance(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<PointTransactionResponse>> transactions(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                pointService.listTransactions(SecurityUtils.getCurrentUserId(), pageable)));
    }
}
