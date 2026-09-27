package com.example.shopping.returns.service;

import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.OrderStatusLog;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.mail.OrderMailSender;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.points.dto.PointBalanceResponse;
import com.example.shopping.points.service.PointService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.returns.dto.request.ReturnApplyRequest;
import com.example.shopping.returns.dto.request.ReturnDecisionRequest;
import com.example.shopping.returns.entity.ReturnRequest;
import com.example.shopping.returns.repository.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnServiceImplTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PointService pointService;
    @Mock
    private OrderMailSender orderMailSender;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private Orders order;
    private Product product;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        Member member = new Member();
        member.setId(1L);

        product = new Product();
        product.setSalesCount(5);
        sku = new ProductSku();
        sku.setProduct(product);
        sku.setStock(3);

        order = new Orders();
        order.setId(7L);
        order.setOrderNo("ORD7");
        order.setMember(member);
        order.setTotalAmount(new BigDecimal("1000"));
        OrderItem item = new OrderItem();
        item.setProductSku(sku);
        item.setQuantity(2);
        order.addItem(item);
        completeAt(LocalDateTime.now().minusDays(1));
    }

    /** 模擬訂單在指定時間完成(狀態歷程的 createdAt 由 DB 產生,測試直接指定) */
    private void completeAt(LocalDateTime completedAt) {
        order.setStatus(OrderStatus.SHIPPING);
        order.changeStatus(OrderStatus.COMPLETED, OrderActor.ADMIN, null);
        OrderStatusLog log = order.getStatusLogs().get(order.getStatusLogs().size() - 1);
        log.setCreatedAt(completedAt);
    }

    private ReturnApplyRequest applyRequest() {
        ReturnApplyRequest request = new ReturnApplyRequest();
        request.setReason("尺寸不合");
        return request;
    }

    private ReturnRequest pendingReturn() {
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setId(3L);
        returnRequest.setOrder(order);
        returnRequest.setReason("尺寸不合");
        order.setReturnRequest(returnRequest);
        return returnRequest;
    }

    @Test
    void apply_createsPendingRequest_withinReturnWindow() {
        when(orderRepository.findByIdAndMemberId(7L, 1L)).thenReturn(Optional.of(order));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = returnService.apply(1L, 7L, applyRequest());

        assertThat(response.getReturnRequest().getStatus()).isEqualTo(ReturnStatus.PENDING);
        assertThat(response.getReturnRequest().getReason()).isEqualTo("尺寸不合");
    }

    @Test
    void apply_throws_afterReturnWindow() {
        order.getStatusLogs().clear();
        completeAt(LocalDateTime.now().minusDays(8));
        when(orderRepository.findByIdAndMemberId(7L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.apply(1L, 7L, applyRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("鑑賞期");
        verify(returnRequestRepository, never()).save(any());
    }

    @Test
    void apply_throws_whenOrderNotCompleted() {
        order.setStatus(OrderStatus.SHIPPING);
        when(orderRepository.findByIdAndMemberId(7L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.apply(1L, 7L, applyRequest()))
                .hasMessageContaining("只有已完成");
    }

    @Test
    void apply_throws_whenAlreadyRequested() {
        pendingReturn();
        when(orderRepository.findByIdAndMemberId(7L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.apply(1L, 7L, applyRequest()))
                .hasMessageContaining("已申請過");
    }

    @Test
    void approve_refundsOrder_restocks_andSettlesPoints() {
        ReturnRequest returnRequest = pendingReturn();
        order.setPointsUsed(50);
        order.setPointsEarned(10);
        when(returnRequestRepository.findById(3L)).thenReturn(Optional.of(returnRequest));
        when(pointService.getBalance(1L)).thenReturn(
                new PointBalanceResponse(60, new BigDecimal("0.01"), new BigDecimal("0.5")));

        returnService.approve(3L, new ReturnDecisionRequest());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        assertThat(returnRequest.getProcessedAt()).isNotNull();
        assertThat(sku.getStock()).isEqualTo(5);
        assertThat(product.getSalesCount()).isEqualTo(3);
        verify(pointService).credit(eq(1L), eq(7L), eq(50), eq(PointTransactionType.REFUND), any());
        verify(pointService).deduct(eq(1L), eq(7L), eq(10), eq(PointTransactionType.ADJUST), any());
        verify(orderMailSender).notifyStatusChanged(order);
    }

    @Test
    void approve_onlyRevokesRemainingBalance_whenEarnedPointsAlreadySpent() {
        ReturnRequest returnRequest = pendingReturn();
        order.setPointsEarned(10);
        when(returnRequestRepository.findById(3L)).thenReturn(Optional.of(returnRequest));
        when(pointService.getBalance(1L)).thenReturn(
                new PointBalanceResponse(4, new BigDecimal("0.01"), new BigDecimal("0.5")));

        returnService.approve(3L, new ReturnDecisionRequest());

        verify(pointService).deduct(eq(1L), eq(7L), eq(4), eq(PointTransactionType.ADJUST), any());
    }

    @Test
    void reject_keepsOrderCompleted_andEmailsReason() {
        ReturnRequest returnRequest = pendingReturn();
        when(returnRequestRepository.findById(3L)).thenReturn(Optional.of(returnRequest));
        ReturnDecisionRequest decision = new ReturnDecisionRequest();
        decision.setNote("商品已拆封使用");

        returnService.reject(3L, decision);

        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.REJECTED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(orderMailSender).notifyReturnRejected(order, "商品已拆封使用");
        verify(pointService, never()).credit(any(), any(), anyInt(), any(), any());
    }

    @Test
    void decisions_throw_whenAlreadyProcessed() {
        ReturnRequest returnRequest = pendingReturn();
        returnRequest.setStatus(ReturnStatus.REJECTED);
        when(returnRequestRepository.findById(3L)).thenReturn(Optional.of(returnRequest));

        assertThatThrownBy(() -> returnService.approve(3L, new ReturnDecisionRequest()))
                .hasMessageContaining("已處理過");
    }
}
