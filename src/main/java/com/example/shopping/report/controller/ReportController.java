package com.example.shopping.report.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.report.dto.CategorySalesResponse;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.DashboardResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.report.dto.TopProductResponse;
import com.example.shopping.report.service.DashboardService;
import com.example.shopping.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;

    public ReportController(ReportService reportService, DashboardService dashboardService) {
        this.reportService = reportService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(dashboardService.getDashboard());
    }

    @GetMapping("/summary")
    public ApiResponse<SalesSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ApiResponse.success(reportService.getSummary(startDate, endDate));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<TopProductResponse>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        return ApiResponse.success(reportService.getTopProducts(startDate, endDate, limit));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategorySalesResponse>> categories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ApiResponse.success(reportService.getCategorySales(startDate, endDate));
    }

    @GetMapping("/daily")
    public ApiResponse<List<DailySalesResponse>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ApiResponse.success(reportService.getDailySales(startDate, endDate));
    }
}
