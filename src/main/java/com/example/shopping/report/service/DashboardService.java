package com.example.shopping.report.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.message.OrderMessageRepository;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import com.example.shopping.question.repository.ProductQuestionRepository;
import com.example.shopping.report.dto.DashboardResponse;
import com.example.shopping.report.dto.SalesSummaryResponse;
import com.example.shopping.returns.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** 與後台庫存警示面板的預設門檻一致 */
    static final int LOW_STOCK_THRESHOLD = 10;

    private final ReportService reportService;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ProductQuestionRepository questionRepository;
    private final ProductSkuRepository productSkuRepository;
    private final OrderMessageRepository orderMessageRepository;

    public DashboardService(ReportService reportService,
                            OrderRepository orderRepository,
                            MemberRepository memberRepository,
                            ReturnRequestRepository returnRequestRepository,
                            ProductQuestionRepository questionRepository,
                            ProductSkuRepository productSkuRepository,
                            OrderMessageRepository orderMessageRepository) {
        this.reportService = reportService;
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.questionRepository = questionRepository;
        this.productSkuRepository = productSkuRepository;
        this.orderMessageRepository = orderMessageRepository;
    }

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        SalesSummaryResponse todaySummary = reportService.getSummary(today, today);

        return new DashboardResponse(
                todaySummary.getTotalOrders(),
                todaySummary.getTotalRevenue(),
                memberRepository.countByCreatedAtGreaterThanEqual(today.atStartOfDay()),
                orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT),
                orderRepository.countByStatus(OrderStatus.PAID),
                returnRequestRepository.countByStatus(ReturnStatus.PENDING),
                questionRepository.countByAnsweredAtIsNull(),
                orderMessageRepository.countAwaitingReply(),
                productSkuRepository.countByProductStatusAndStockLessThanEqual(ProductStatus.ON_SALE, LOW_STOCK_THRESHOLD),
                LOW_STOCK_THRESHOLD);
    }
}
