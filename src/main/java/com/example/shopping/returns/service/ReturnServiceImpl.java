package com.example.shopping.returns.service;

import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.mail.OrderNotifier;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.points.service.PointService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import com.example.shopping.returns.dto.request.ReturnApplyRequest;
import com.example.shopping.returns.dto.request.ReturnDecisionRequest;
import com.example.shopping.returns.dto.response.ReturnRequestResponse;
import com.example.shopping.returns.entity.ReturnRequest;
import com.example.shopping.returns.repository.ReturnRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final PointService pointService;
    private final OrderNotifier orderNotifier;
    private final ProductSkuRepository productSkuRepository;

    public ReturnServiceImpl(ReturnRequestRepository returnRequestRepository,
                             OrderRepository orderRepository,
                             PointService pointService,
                             OrderNotifier orderNotifier,
                             ProductSkuRepository productSkuRepository) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.pointService = pointService;
        this.orderNotifier = orderNotifier;
        this.productSkuRepository = productSkuRepository;
    }

    @Override
    public OrderResponse apply(Long memberId, Long orderId, ReturnApplyRequest request) {
        Orders order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("只有已完成的訂單可以申請退貨");
        }
        if (order.getReturnRequest() != null || returnRequestRepository.existsByOrderId(orderId)) {
            throw new BusinessException("此訂單已申請過退貨");
        }
        LocalDateTime deadline = order.getReturnDeadline();
        if (deadline == null || LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException("已超過 " + ReturnRequest.RETURN_WINDOW.toDays() + " 天鑑賞期,無法申請退貨");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setReason(request.getReason().trim());
        order.setReturnRequest(returnRequestRepository.save(returnRequest));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listAdmin(ReturnStatus status, Pageable pageable) {
        Page<ReturnRequest> page = status == null
                ? returnRequestRepository.findAll(pageable)
                : returnRequestRepository.findByStatus(status, pageable);
        return page.map(ReturnRequestResponse::from);
    }

    @Override
    public ReturnRequestResponse approve(Long returnId, ReturnDecisionRequest request) {
        ReturnRequest returnRequest = findPendingOrThrow(returnId);
        Orders order = returnRequest.getOrder();
        Long memberId = order.getMember().getId();

        for (OrderItem item : order.getItems()) {
            ProductSku sku = item.getProductSku();
            productSkuRepository.incrementStock(sku.getId(), item.getQuantity());
            Product product = sku.getProduct();
            product.setSalesCount(Math.max(0, product.getSalesCount() - item.getQuantity()));
        }
        if (order.getPointsUsed() > 0) {
            pointService.credit(memberId, order.getId(), order.getPointsUsed(), PointTransactionType.REFUND,
                    "訂單 " + order.getOrderNo() + " 退貨退還");
        }
        revokeEarnedPoints(order, memberId);

        String note = blankToNull(request.getNote());
        returnRequest.setStatus(ReturnStatus.APPROVED);
        returnRequest.setAdminNote(note);
        returnRequest.setProcessedAt(LocalDateTime.now());
        order.changeStatus(OrderStatus.REFUNDED, OrderActor.ADMIN, note != null ? note : "退貨核准,已退款");
        orderNotifier.notifyStatusChanged(order);
        return ReturnRequestResponse.from(returnRequest);
    }

    @Override
    public ReturnRequestResponse reject(Long returnId, ReturnDecisionRequest request) {
        ReturnRequest returnRequest = findPendingOrThrow(returnId);
        String note = blankToNull(request.getNote());
        returnRequest.setStatus(ReturnStatus.REJECTED);
        returnRequest.setAdminNote(note);
        returnRequest.setProcessedAt(LocalDateTime.now());
        orderNotifier.notifyReturnRejected(returnRequest.getOrder(), note);
        return ReturnRequestResponse.from(returnRequest);
    }

    /**
     * 收回訂單完成時回饋的購物金。若會員已經花掉一部分,只收回剩餘的餘額,不讓餘額變成負數。
     */
    private void revokeEarnedPoints(Orders order, Long memberId) {
        int earned = order.getPointsEarned();
        if (earned <= 0) {
            return;
        }
        int revocable = Math.min(earned, pointService.getBalance(memberId).getBalance());
        if (revocable > 0) {
            pointService.deduct(memberId, order.getId(), revocable, PointTransactionType.ADJUST,
                    "訂單 " + order.getOrderNo() + " 退貨,收回回饋購物金");
        }
    }

    private ReturnRequest findPendingOrThrow(Long returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("退貨申請不存在"));
        if (returnRequest.getStatus() != ReturnStatus.PENDING) {
            throw new BusinessException("此退貨申請已處理過");
        }
        return returnRequest;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
