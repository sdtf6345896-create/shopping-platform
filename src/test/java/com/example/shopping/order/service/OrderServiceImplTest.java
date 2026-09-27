package com.example.shopping.order.service;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.service.CouponService;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.mail.OrderMailSender;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private OrderMailSender orderMailSender;
    @Mock
    private OrderPaymentPolicy paymentPolicy;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Address address;
    private Product product;
    private ProductSku sku;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        Member member = new Member();
        member.setId(1L);

        address = new Address();
        address.setId(2L);
        address.setMember(member);
        address.setRecipientName("王小明");
        address.setPhone("0912345678");
        address.setCity("台北市");
        address.setDistrict("大安區");
        address.setDetailAddress("復興南路一段1號");

        product = new Product();
        product.setId(10L);
        product.setName("經典圓領T恤");
        product.setStatus(ProductStatus.ON_SALE);
        product.setSalesCount(0);

        sku = new ProductSku();
        sku.setId(1L);
        sku.setProduct(product);
        sku.setSpecName("黑色/M");
        sku.setPrice(new BigDecimal("590.00"));
        sku.setStock(5);

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setProductSku(sku);
        cartItem.setQuantity(2);
    }

    private CheckoutRequest checkoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId(2L);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        return request;
    }

    private Orders pendingOrderWithItem(int quantity) {
        Orders order = new Orders();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        OrderItem item = new OrderItem();
        item.setProductSku(sku);
        item.setQuantity(quantity);
        order.addItem(item);
        return order;
    }

    @Test
    void checkout_decrementsStockAndClearsUsedCartItems() {
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.checkout(1L, checkoutRequest());

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1180.00"));
        assertThat(sku.getStock()).isEqualTo(3);
        verify(cartItemRepository).deleteAll(List.of(cartItem));
        verify(orderMailSender).notifyStatusChanged(any(Orders.class));
    }

    @Test
    void checkout_appliesCouponDiscount_whenCouponCodeProvided() {
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));

        Coupon coupon = new Coupon();
        coupon.setId(5L);
        coupon.setCode("SAVE100");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(new BigDecimal("100"));

        when(couponService.reserve("SAVE100", new BigDecimal("1180.00"))).thenReturn(
                new CouponApplyResponse(5L, "SAVE100", "折抵 100 元", DiscountType.FIXED_AMOUNT,
                        new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1080.00")));
        when(couponRepository.getReferenceById(5L)).thenReturn(coupon);

        CheckoutRequest request = checkoutRequest();
        request.setCouponCode("SAVE100");

        OrderResponse response = orderService.checkout(1L, request);

        assertThat(response.getSubtotalAmount()).isEqualByComparingTo("1180.00");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("100");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1080.00");
        assertThat(response.getCouponCode()).isEqualTo("SAVE100");
    }

    @Test
    void checkout_throws_whenCartIsEmpty() {
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(1L, checkoutRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("購物車是空的");
        verify(orderMailSender, never()).notifyStatusChanged(any());
    }

    @Test
    void checkout_throws_whenAddressNotOwnedByMember() {
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout(1L, checkoutRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("收件地址不存在");
    }

    @Test
    void checkout_throws_whenProductOffShelf() {
        product.setStatus(ProductStatus.OFF_SHELF);
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.checkout(1L, checkoutRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已下架");
    }

    @Test
    void checkout_throws_whenStockInsufficient() {
        cartItem.setQuantity(99);
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.checkout(1L, checkoutRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("庫存不足");
    }

    @Test
    void pay_marksOrderPaidAndIncrementsSalesCount() {
        Orders order = pendingOrderWithItem(2);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.pay(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(product.getSalesCount()).isEqualTo(2);
        verify(orderMailSender).notifyStatusChanged(order);
    }

    @Test
    void checkout_setsPaymentDeadlineFromPolicy() {
        LocalDateTime deadline = LocalDateTime.of(2026, 9, 27, 16, 0);
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentPolicy.deadlineFor(eq(PaymentMethod.CREDIT_CARD), any(LocalDateTime.class))).thenReturn(deadline);

        OrderResponse response = orderService.checkout(1L, checkoutRequest());

        assertThat(response.getPaymentDeadline()).isEqualTo(deadline);
    }

    @Test
    void pay_throws_whenPaymentDeadlinePassed() {
        Orders order = pendingOrderWithItem(2);
        order.setPaymentDeadline(LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.pay(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("付款期限");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void cancelExpiredOrders_cancelsAndRestoresStockAsSystem() {
        sku.setStock(3);
        Orders order = pendingOrderWithItem(2);
        Coupon coupon = new Coupon();
        coupon.setId(5L);
        order.setCoupon(coupon);
        LocalDateTime now = LocalDateTime.now();
        when(orderRepository.findByStatusAndPaymentDeadlineBefore(OrderStatus.PENDING_PAYMENT, now))
                .thenReturn(List.of(order));

        int cancelled = orderService.cancelExpiredOrders(now);

        assertThat(cancelled).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sku.getStock()).isEqualTo(5);
        assertThat(order.getStatusLogs()).last()
                .satisfies(log -> assertThat(log.getActor()).isEqualTo(OrderActor.SYSTEM));
        verify(couponService).release(5L);
        verify(orderMailSender).notifyStatusChanged(order);
    }

    @Test
    void pay_throws_whenOrderNotPendingPayment() {
        Orders order = pendingOrderWithItem(2);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.pay(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無法付款");
        verify(orderMailSender, never()).notifyStatusChanged(any());
    }

    @Test
    void cancelByMember_restoresStock_whenPendingPayment() {
        sku.setStock(3);
        Orders order = pendingOrderWithItem(2);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelByMember(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sku.getStock()).isEqualTo(5);
        verify(orderMailSender).notifyStatusChanged(order);
    }

    @Test
    void cancelByMember_releasesCouponQuota_whenOrderHadCoupon() {
        sku.setStock(3);
        Orders order = pendingOrderWithItem(2);
        Coupon coupon = new Coupon();
        coupon.setId(5L);
        order.setCoupon(coupon);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        orderService.cancelByMember(1L, 1L);

        verify(couponService).release(5L);
    }

    @Test
    void cancelByMember_throws_whenOrderAlreadyShipping() {
        Orders order = pendingOrderWithItem(2);
        order.setStatus(OrderStatus.SHIPPING);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelByMember(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無法取消");
    }

    @Test
    void adminUpdateStatus_allowsValidTransition_paidToShipping() {
        Orders order = pendingOrderWithItem(1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.SHIPPING);
        request.setShippingCarrier("黑貓宅急便");
        request.setTrackingNumber("TRK123456");

        OrderResponse response = orderService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(response.getShippingCarrier()).isEqualTo("黑貓宅急便");
        assertThat(response.getTrackingNumber()).isEqualTo("TRK123456");
        assertThat(response.getShippedAt()).isNotNull();
        verify(orderMailSender).notifyStatusChanged(order);
    }

    @Test
    void adminUpdateStatus_toShipping_throws_whenTrackingNumberMissing() {
        Orders order = pendingOrderWithItem(1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.SHIPPING);
        request.setShippingCarrier("黑貓宅急便");
        request.setTrackingNumber("  ");

        assertThatThrownBy(() -> orderService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("物流單號");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderMailSender, never()).notifyStatusChanged(any());
    }

    @Test
    void checkout_recordsInitialStatusLog() {
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.checkout(1L, checkoutRequest());

        assertThat(response.getStatusLogs()).hasSize(1);
        assertThat(response.getStatusLogs().get(0).getFromStatus()).isNull();
        assertThat(response.getStatusLogs().get(0).getToStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.getStatusLogs().get(0).getActor()).isEqualTo(OrderActor.MEMBER);
    }

    @Test
    void statusChanges_appendLogsWithActorAndNote() {
        Orders order = pendingOrderWithItem(1);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.pay(1L, 1L);
        OrderStatusRequest cancel = new OrderStatusRequest();
        cancel.setStatus(OrderStatus.CANCELLED);
        cancel.setNote("缺貨無法出貨");
        OrderResponse response = orderService.updateStatus(1L, cancel);

        assertThat(response.getStatusLogs()).extracting("toStatus")
                .containsExactly(OrderStatus.PAID, OrderStatus.CANCELLED);
        assertThat(response.getStatusLogs()).extracting("actor")
                .containsExactly(OrderActor.MEMBER, OrderActor.ADMIN);
        assertThat(response.getStatusLogs().get(1).getFromStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.getStatusLogs().get(1).getNote()).isEqualTo("缺貨無法出貨");
    }

    @Test
    void adminUpdateStatus_throws_whenSkippingShippingStep() {
        Orders order = pendingOrderWithItem(1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        assertThatThrownBy(() -> orderService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允許");
    }

    @Test
    void adminUpdateStatus_toCancelled_restoresStockWithoutTouchingSalesCount() {
        sku.setStock(3);
        Orders order = pendingOrderWithItem(2);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.CANCELLED);

        OrderResponse response = orderService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sku.getStock()).isEqualTo(5);
        assertThat(product.getSalesCount()).isEqualTo(0);
    }
}
