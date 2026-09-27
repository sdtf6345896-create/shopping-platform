package com.example.shopping.order.service;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.service.CouponService;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.tier.MemberTier;
import com.example.shopping.member.tier.MemberTierService;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.dto.response.ReorderResponse;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.mail.OrderNotifier;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.order.shipping.ShippingPolicy;
import com.example.shopping.points.dto.PointBalanceResponse;
import com.example.shopping.points.service.PointPolicy;
import com.example.shopping.points.service.PointService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
    private OrderNotifier orderNotifier;
    @Mock
    private OrderPaymentPolicy paymentPolicy;
    @Mock
    private ProductSkuRepository productSkuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private MemberTierService memberTierService;
    @Spy
    private ShippingPolicy shippingPolicy = new ShippingPolicy(new BigDecimal("60"), new BigDecimal("999"));
    @Mock
    private PointService pointService;
    @Spy
    private PointPolicy pointPolicy = new PointPolicy(new BigDecimal("0.01"), new BigDecimal("0.5"));

    @InjectMocks
    private OrderServiceImpl orderService;

    private Address address;
    private Product product;
    private ProductSku sku;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        // 預設扣庫存成功;搶輸最後一件的情境由個別測試覆寫
        lenient().when(productSkuRepository.decrementStock(anyLong(), anyInt())).thenReturn(1);
        lenient().when(memberTierService.tierOf(any())).thenReturn(MemberTier.NORMAL);

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
        assertThat(response.getShippingFee()).isZero();
        verify(productSkuRepository).decrementStock(1L, 2);
        verify(cartItemRepository).deleteAll(List.of(cartItem));
        verify(orderNotifier).notifyStatusChanged(any(Orders.class));
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

        when(couponService.reserve("SAVE100", new BigDecimal("1180.00"), 1L)).thenReturn(
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
        verify(orderNotifier, never()).notifyStatusChanged(any());
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
    void checkout_throws_whenAnotherBuyerTookTheLastUnit() {
        // 讀到的庫存還夠,但條件式 UPDATE 扣不到(別人同時結帳先扣走了)
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
        when(productSkuRepository.decrementStock(1L, 2)).thenReturn(0);

        assertThatThrownBy(() -> orderService.checkout(1L, checkoutRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("庫存不足");
        verify(orderRepository, never()).save(any());
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
        verify(productRepository).addSalesCount(10L, 2);
        verify(orderNotifier).notifyStatusChanged(order);
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
        verify(productSkuRepository).incrementStock(1L, 2);
        assertThat(order.getStatusLogs()).last()
                .satisfies(log -> assertThat(log.getActor()).isEqualTo(OrderActor.SYSTEM));
        verify(couponService).release(5L);
        verify(orderNotifier).notifyStatusChanged(order);
    }

    @Test
    void pay_throws_whenOrderNotPendingPayment() {
        Orders order = pendingOrderWithItem(2);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.pay(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無法付款");
        verify(orderNotifier, never()).notifyStatusChanged(any());
    }

    @Test
    void cancelByMember_restoresStock_whenPendingPayment() {
        sku.setStock(3);
        Orders order = pendingOrderWithItem(2);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelByMember(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productSkuRepository).incrementStock(1L, 2);
        verify(orderNotifier).notifyStatusChanged(order);
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
        verify(orderNotifier).notifyStatusChanged(order);
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
        verify(orderNotifier, never()).notifyStatusChanged(any());
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
        verify(productSkuRepository).incrementStock(1L, 2);
        verify(productRepository, never()).addSalesCount(any(), anyInt());
    }

    @Test
    void reorder_addsItemsToCart_andMergesWithExistingCartQuantity() {
        Orders order = pendingOrderWithItem(2);
        order.getItems().get(0).setProductName("經典圓領T恤");
        order.getItems().get(0).setSpecName("黑色/M");
        CartItem existing = new CartItem();
        existing.setProductSku(sku);
        existing.setQuantity(1);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));
        when(cartItemRepository.findByMemberIdAndProductSkuId(1L, 1L)).thenReturn(Optional.of(existing));

        ReorderResponse response = orderService.reorder(1L, 1L);

        assertThat(response.getAddedCount()).isEqualTo(1);
        assertThat(response.getNotices()).isEmpty();
        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void reorder_addsOnlyAvailableQuantity_whenStockLow() {
        sku.setStock(1);
        Orders order = pendingOrderWithItem(2);
        order.getItems().get(0).setProductName("經典圓領T恤");
        order.getItems().get(0).setSpecName("黑色/M");
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));
        when(cartItemRepository.findByMemberIdAndProductSkuId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ReorderResponse response = orderService.reorder(1L, 1L);

        assertThat(response.getAddedCount()).isEqualTo(1);
        assertThat(response.getNotices()).singleElement().asString().contains("僅加入 1 件");
    }

    @Test
    void reorder_skipsOffShelfAndSoldOutItems() {
        product.setStatus(ProductStatus.OFF_SHELF);
        Orders order = pendingOrderWithItem(1);
        order.getItems().get(0).setProductName("經典圓領T恤");
        order.getItems().get(0).setSpecName("黑色/M");
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        ReorderResponse response = orderService.reorder(1L, 1L);

        assertThat(response.getAddedCount()).isZero();
        assertThat(response.getNotices()).singleElement().asString().contains("已下架");
        verify(cartItemRepository, never()).save(any());
    }

    private void stubCheckout() {
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
    }

    private void stubBalance(int balance) {
        when(pointService.getBalance(1L)).thenReturn(
                new PointBalanceResponse(balance, new BigDecimal("0.01"), new BigDecimal("0.5")));
    }

    @Test
    void checkout_deductsPointsFromTotal() {
        stubCheckout();
        stubBalance(500);
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> {
            Orders o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });
        CheckoutRequest request = checkoutRequest();
        request.setPointsToUse(200);

        OrderResponse response = orderService.checkout(1L, request);

        assertThat(response.getPointsUsed()).isEqualTo(200);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("980.00");
        verify(pointService).deduct(eq(1L), eq(99L), eq(200), eq(PointTransactionType.REDEEM), any());
    }

    @Test
    void checkout_rejectsPointsAboveHalfOfPayable() {
        stubCheckout();
        stubBalance(5000);
        CheckoutRequest request = checkoutRequest();
        request.setPointsToUse(591); // 應付 1180,上限 590

        assertThatThrownBy(() -> orderService.checkout(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多可折抵 590 點");
        verify(orderRepository, never()).save(any());
        verify(pointService, never()).deduct(any(), any(), anyInt(), any(), any());
    }

    @Test
    void checkout_rejectsPointsAboveBalance() {
        stubCheckout();
        stubBalance(100);
        CheckoutRequest request = checkoutRequest();
        request.setPointsToUse(101);

        assertThatThrownBy(() -> orderService.checkout(1L, request))
                .hasMessageContaining("最多可折抵 100 點");
    }

    @Test
    void cancel_refundsUsedPoints() {
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setOrderNo("ORD1");
        order.setPointsUsed(150);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        orderService.cancelByMember(1L, 1L);

        verify(pointService).credit(eq(1L), eq(1L), eq(150), eq(PointTransactionType.REFUND), any());
    }

    @Test
    void complete_earnsOnePercentOfPaidAmount() {
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setOrderNo("ORD1");
        order.setStatus(OrderStatus.SHIPPING);
        order.setTotalAmount(new BigDecimal("1080.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        orderService.updateStatus(1L, request);

        verify(pointService).credit(eq(1L), eq(1L), eq(10), eq(PointTransactionType.EARN), any());
    }

    @Test
    void complete_earnsNothing_forTinyOrders() {
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setStatus(OrderStatus.SHIPPING);
        order.setTotalAmount(new BigDecimal("99.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        orderService.updateStatus(1L, request);

        verify(pointService, never()).credit(any(), any(), anyInt(), any(), any());
    }

    @Test
    void checkout_chargesFlashSalePrice_andSnapshotsIt() {
        product.setSaleDiscountPercent(20);
        product.setSaleStartAt(LocalDateTime.now().minusHours(1));
        product.setSaleEndAt(LocalDateTime.now().plusHours(1));
        when(addressRepository.findByIdAndMemberId(2L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findAllByMemberIdWithDetails(1L)).thenReturn(List.of(cartItem));
        when(memberRepository.getReferenceById(1L)).thenReturn(address.getMember());
        when(orderRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.checkout(1L, checkoutRequest());

        // 590 × 80% = 472,兩件 944;未滿 999 免運門檻,加運費 60
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("472.00");
        assertThat(response.getShippingFee()).isEqualByComparingTo("60");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1004.00");
    }

    @Test
    void complete_earnsPointsOnMerchandiseOnly_notShippingFee() {
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setOrderNo("ORD1");
        order.setStatus(OrderStatus.SHIPPING);
        order.setShippingFee(new BigDecimal("60"));
        order.setTotalAmount(new BigDecimal("560"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        orderService.updateStatus(1L, request);

        verify(pointService).credit(eq(1L), eq(1L), eq(5), eq(PointTransactionType.EARN), any());
    }

    @Test
    void complete_goldMemberEarnsDoublePoints() {
        when(memberTierService.tierOf(1L)).thenReturn(MemberTier.GOLD);
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setOrderNo("ORD1");
        order.setStatus(OrderStatus.SHIPPING);
        order.setTotalAmount(new BigDecimal("1000"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        orderService.updateStatus(1L, request);

        assertThat(order.getPointsEarned()).isEqualTo(20);
        verify(pointService).credit(eq(1L), eq(1L), eq(20), eq(PointTransactionType.EARN),
                argThat(description -> description.contains("金卡會員 2 倍")));
    }

    @Test
    void confirmReceipt_completesShippingOrder_asMember_andEarnsPoints() {
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setOrderNo("ORD1");
        order.setStatus(OrderStatus.SHIPPING);
        order.setTotalAmount(new BigDecimal("1000"));
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.confirmReceipt(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getStatusLogs()).last().satisfies(log -> assertThat(log.getActor()).isEqualTo(OrderActor.MEMBER));
        verify(pointService).credit(eq(1L), eq(1L), eq(10), eq(PointTransactionType.EARN), any());
        verify(orderNotifier).notifyStatusChanged(order);
    }

    @Test
    void confirmReceipt_rejectsOrdersNotYetShipped() {
        Orders order = pendingOrderWithItem(1);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmReceipt(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("出貨中");
        verify(pointService, never()).credit(any(), any(), anyInt(), any(), any());
    }

    @Test
    void autoCompleteShipped_completesOldShipmentsAsSystem() {
        Orders order = pendingOrderWithItem(1);
        order.setMember(address.getMember());
        order.setOrderNo("ORD1");
        order.setStatus(OrderStatus.SHIPPING);
        order.setTotalAmount(new BigDecimal("500"));
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        when(orderRepository.findByStatusAndShippedAtBefore(OrderStatus.SHIPPING, cutoff)).thenReturn(List.of(order));

        int completed = orderService.autoCompleteShipped(cutoff);

        assertThat(completed).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getStatusLogs()).last().satisfies(log -> assertThat(log.getActor()).isEqualTo(OrderActor.SYSTEM));
        verify(pointService).credit(eq(1L), eq(1L), eq(5), eq(PointTransactionType.EARN), any());
    }
}
