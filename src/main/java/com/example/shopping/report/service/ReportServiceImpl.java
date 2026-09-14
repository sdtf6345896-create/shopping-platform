package com.example.shopping.report.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.report.dto.TopProductResponse;
import com.example.shopping.report.repository.OrderStatusStatProjection;
import com.example.shopping.report.repository.ReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final List<OrderStatus> REVENUE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final ReportRepository reportRepository;

    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public SalesSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveRange(startDate, endDate);
        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].atTime(LocalTime.MAX);

        List<OrderStatusStatProjection> stats = reportRepository.findStatusStats(start, end);

        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status, 0L);
        }

        long totalOrders = 0;
        long paidOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (OrderStatusStatProjection stat : stats) {
            statusCounts.put(stat.getStatus(), stat.getCount());
            totalOrders += stat.getCount();
            if (REVENUE_STATUSES.contains(stat.getStatus())) {
                paidOrders += stat.getCount();
                totalRevenue = totalRevenue.add(stat.getAmount());
            }
        }

        BigDecimal averageOrderValue = paidOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(paidOrders), 2, java.math.RoundingMode.HALF_UP);

        return new SalesSummaryResponse(
                range[0], range[1], totalOrders, paidOrders,
                statusCounts.getOrDefault(OrderStatus.CANCELLED, 0L),
                totalRevenue, averageOrderValue, statusCounts);
    }

    @Override
    public List<TopProductResponse> getTopProducts(LocalDate startDate, LocalDate endDate, int limit) {
        LocalDate[] range = resolveRange(startDate, endDate);
        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].atTime(LocalTime.MAX);

        return reportRepository.findTopProducts(REVENUE_STATUSES, start, end, PageRequest.of(0, limit)).stream()
                .map(TopProductResponse::from)
                .toList();
    }

    @Override
    public List<DailySalesResponse> getDailySales(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveRange(startDate, endDate);
        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].atTime(LocalTime.MAX);

        List<String> statusNames = REVENUE_STATUSES.stream().map(Enum::name).toList();
        return reportRepository.findDailyStats(statusNames, start, end).stream()
                .map(DailySalesResponse::from)
                .toList();
    }

    private LocalDate[] resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_RANGE_DAYS - 1);

        if (start.isAfter(end)) {
            throw new BusinessException("開始日期不可晚於結束日期");
        }
        return new LocalDate[] { start, end };
    }
}
