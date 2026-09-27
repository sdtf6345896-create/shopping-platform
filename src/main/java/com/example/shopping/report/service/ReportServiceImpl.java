package com.example.shopping.report.service;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.report.dto.CategorySalesResponse;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.report.dto.TopProductResponse;
import com.example.shopping.report.repository.CategorySalesProjection;
import com.example.shopping.report.repository.OrderStatusStatProjection;
import com.example.shopping.report.repository.ReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final List<OrderStatus> REVENUE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;

    public ReportServiceImpl(ReportRepository reportRepository, CategoryRepository categoryRepository) {
        this.reportRepository = reportRepository;
        this.categoryRepository = categoryRepository;
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

    @Override
    public List<CategorySalesResponse> getCategorySales(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveRange(startDate, endDate);
        List<CategorySalesProjection> rows = reportRepository.findCategorySales(REVENUE_STATUSES,
                range[0].atStartOfDay(), range[1].atTime(LocalTime.MAX));

        Map<Long, Category> categories = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categories.put(c.getId(), c));

        // 子分類的銷售往上併入頂層分類
        Map<Long, long[]> quantityByRoot = new LinkedHashMap<>();
        Map<Long, BigDecimal> revenueByRoot = new HashMap<>();
        for (CategorySalesProjection row : rows) {
            Category root = rootOf(categories.get(row.getCategoryId()));
            Long rootId = root == null ? row.getCategoryId() : root.getId();
            quantityByRoot.computeIfAbsent(rootId, id -> new long[1])[0] += row.getSoldQuantity();
            revenueByRoot.merge(rootId, row.getRevenue(), BigDecimal::add);
        }

        BigDecimal total = revenueByRoot.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return quantityByRoot.keySet().stream()
                .map(rootId -> {
                    BigDecimal revenue = revenueByRoot.get(rootId);
                    BigDecimal share = total.signum() == 0 ? BigDecimal.ZERO
                            : revenue.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
                    Category category = categories.get(rootId);
                    return new CategorySalesResponse(rootId, category == null ? "(已刪除分類)" : category.getName(),
                            quantityByRoot.get(rootId)[0], revenue, share);
                })
                .sorted(Comparator.comparing(CategorySalesResponse::revenue).reversed())
                .toList();
    }

    private static Category rootOf(Category category) {
        Category current = category;
        // 最多往上找 10 層,避免資料異常形成循環時無窮迴圈
        for (int depth = 0; current != null && current.getParent() != null && depth < 10; depth++) {
            current = current.getParent();
        }
        return current;
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
