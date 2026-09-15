package com.example.shopping.report.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.report.dto.TopProductResponse;
import com.example.shopping.report.repository.DailyStatProjection;
import com.example.shopping.report.repository.OrderStatusStatProjection;
import com.example.shopping.report.repository.ReportRepository;
import com.example.shopping.report.repository.TopProductProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static OrderStatusStatProjection stat(OrderStatus status, long count, String amount) {
        return new OrderStatusStatProjection() {
            @Override
            public OrderStatus getStatus() {
                return status;
            }

            @Override
            public long getCount() {
                return count;
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(amount);
            }
        };
    }

    @Test
    void getSummary_countsOnlyPaidShippingCompletedAsRevenue() {
        when(reportRepository.findStatusStats(any(), any())).thenReturn(List.of(
                stat(OrderStatus.PENDING_PAYMENT, 2, "0"),
                stat(OrderStatus.PAID, 1, "590.00"),
                stat(OrderStatus.COMPLETED, 3, "3540.00"),
                stat(OrderStatus.CANCELLED, 1, "0")
        ));

        SalesSummaryResponse response = reportService.getSummary(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14));

        assertThat(response.getTotalOrders()).isEqualTo(7);
        assertThat(response.getPaidOrders()).isEqualTo(4);
        assertThat(response.getCancelledOrders()).isEqualTo(1);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("4130.00"));
        assertThat(response.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("1032.50"));
    }

    @Test
    void getSummary_defaultsToPast30Days_whenNoDatesGiven() {
        when(reportRepository.findStatusStats(any(), any())).thenReturn(List.of());

        SalesSummaryResponse response = reportService.getSummary(null, null);

        assertThat(response.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(response.getStartDate()).isEqualTo(LocalDate.now().minusDays(29));
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSummary_throws_whenStartDateAfterEndDate() {
        assertThatThrownBy(() -> reportService.getSummary(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可晚於");
    }

    @Test
    void getTopProducts_mapsProjectionsToResponses() {
        TopProductProjection projection = new TopProductProjection() {
            @Override
            public Long getProductId() {
                return 1L;
            }

            @Override
            public String getProductName() {
                return "經典圓領T恤";
            }

            @Override
            public String getMainImage() {
                return "https://example.com/t.jpg";
            }

            @Override
            public Long getSoldQuantity() {
                return 6L;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal("3540.00");
            }
        };
        when(reportRepository.findTopProducts(any(), any(), any(), any())).thenReturn(List.of(projection));

        List<TopProductResponse> result = reportService.getTopProducts(null, null, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("經典圓領T恤");
        assertThat(result.get(0).getSoldQuantity()).isEqualTo(6L);
    }

    @Test
    void getDailySales_mapsProjectionsToResponses() {
        DailyStatProjection projection = new DailyStatProjection() {
            @Override
            public Date getDay() {
                return Date.valueOf(LocalDate.of(2026, 9, 14));
            }

            @Override
            public Long getOrderCount() {
                return 2L;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal("2360.00");
            }
        };
        when(reportRepository.findDailyStats(any(), any(), any())).thenReturn(List.of(projection));

        List<DailySalesResponse> result = reportService.getDailySales(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(result.get(0).getOrderCount()).isEqualTo(2L);
    }
}
