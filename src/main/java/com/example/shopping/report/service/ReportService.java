package com.example.shopping.report.service;

import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.report.dto.TopProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    SalesSummaryResponse getSummary(LocalDate startDate, LocalDate endDate);

    List<TopProductResponse> getTopProducts(LocalDate startDate, LocalDate endDate, int limit);

    List<DailySalesResponse> getDailySales(LocalDate startDate, LocalDate endDate);
}
