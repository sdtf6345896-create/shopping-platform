package com.example.shopping.report.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.report.dto.CategorySalesResponse;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.DashboardResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.report.dto.TopProductResponse;
import com.example.shopping.report.export.ReportCsvExporter;
import com.example.shopping.report.service.DashboardService;
import com.example.shopping.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;
    private final ReportCsvExporter reportCsvExporter;

    public ReportController(ReportService reportService, DashboardService dashboardService,
                            ReportCsvExporter reportCsvExporter) {
        this.reportService = reportService;
        this.dashboardService = dashboardService;
        this.reportCsvExporter = reportCsvExporter;
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

    /** 匯出報表 CSV:type 為 daily(每日營收)、products(熱銷商品前 100 名)、categories(分類銷售) */
    @AdminAudit(action = "匯出報表 CSV", target = AuditTarget.ORDER, targetId = "",
            detail = "#type + ' ' + #startDate + '~' + #endDate")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportCsvExporter.Type reportType = ReportCsvExporter.parseType(type);
        String filename = "report-" + reportType.name().toLowerCase() + "-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(reportCsvExporter.export(reportType, startDate, endDate));
    }
}
