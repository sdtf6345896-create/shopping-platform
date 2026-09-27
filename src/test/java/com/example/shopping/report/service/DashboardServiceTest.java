package com.example.shopping.report.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import com.example.shopping.question.repository.ProductQuestionRepository;
import com.example.shopping.report.dto.DashboardResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.returns.repository.ReturnRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ReportService reportService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private ProductQuestionRepository questionRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboard_combinesTodaySalesWithPendingWorkCounts() {
        LocalDate today = LocalDate.now();
        when(reportService.getSummary(today, today)).thenReturn(new SalesSummaryResponse(
                today, today, 4, 3, 1, new BigDecimal("2500"), new BigDecimal("833.33"), Map.of()));
        when(memberRepository.countByCreatedAtGreaterThanEqual(today.atStartOfDay())).thenReturn(2L);
        when(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT)).thenReturn(1L);
        when(orderRepository.countByStatus(OrderStatus.PAID)).thenReturn(5L);
        when(returnRequestRepository.countByStatus(ReturnStatus.PENDING)).thenReturn(1L);
        when(questionRepository.countByAnsweredAtIsNull()).thenReturn(3L);
        when(productSkuRepository.countByProductStatusAndStockLessThanEqual(ProductStatus.ON_SALE, 10)).thenReturn(6L);

        DashboardResponse dashboard = dashboardService.getDashboard();

        assertThat(dashboard.getTodayOrders()).isEqualTo(4);
        assertThat(dashboard.getTodayRevenue()).isEqualByComparingTo("2500");
        assertThat(dashboard.getTodayNewMembers()).isEqualTo(2);
        assertThat(dashboard.getPendingPaymentOrders()).isEqualTo(1);
        assertThat(dashboard.getOrdersToShip()).isEqualTo(5);
        assertThat(dashboard.getPendingReturns()).isEqualTo(1);
        assertThat(dashboard.getUnansweredQuestions()).isEqualTo(3);
        assertThat(dashboard.getLowStockSkus()).isEqualTo(6);
        assertThat(dashboard.getLowStockThreshold()).isEqualTo(10);
    }
}
