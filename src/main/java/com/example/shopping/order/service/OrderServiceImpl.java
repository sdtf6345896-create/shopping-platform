package com.example.shopping.order.service;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.service.CouponService;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.tier.MemberTier;
import com.example.shopping.member.tier.MemberTierService;
import com.example.shopping.order.dto.request.AdminOrderQuery;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.dto.response.ReorderResponse;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.export.OrderCsvWriter;
import com.example.shopping.order.mail.OrderNotifier;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.order.shipping.ShippingPolicy;
import com.example.shopping.points.service.PointPolicy;
import com.example.shopping.points.service.PointService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.shopping.order.repository.OrderSpecifications.createdBetween;
import static com.example.shopping.order.repository.OrderSpecifications.hasMemberId;
import static com.example.shopping.order.repository.OrderSpecifications.hasStatus;
import static com.example.shopping.order.repository.OrderSpecifications.keywordMatches;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    static final int MAX_EXPORT_ROWS = 10_000;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPING, EnumSet.of(OrderStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        // REFUNDED 只能經由退貨核准(ReturnService)進入,不開放後台直接切換
        ALLOWED_TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final OrderNotifier orderNotifier;
    private final OrderPaymentPolicy paymentPolicy;
    private final PointService pointService;
    private final PointPolicy pointPolicy;
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final ShippingPolicy shippingPolicy;
    private final MemberTierService memberTierService;

    public OrderServiceImpl(OrderRepository orderRepository,
                             CartItemRepository cartItemRepository,
                             AddressRepository addressRepository,
                             MemberRepository memberRepository,
                             CouponService couponService,
                             CouponRepository couponRepository,
                             OrderNotifier orderNotifier,
                             OrderPaymentPolicy paymentPolicy,
                             PointService pointService,
                             PointPolicy pointPolicy,
                             ProductSkuRepository productSkuRepository,
                             ProductRepository productRepository,
                             ShippingPolicy shippingPolicy,
                             MemberTierService memberTierService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.memberRepository = memberRepository;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
        this.orderNotifier = orderNotifier;
        this.paymentPolicy = paymentPolicy;
        this.pointService = pointService;
        this.pointPolicy = pointPolicy;
        this.productSkuRepository = productSkuRepository;
        this.productRepository = productRepository;
        this.shippingPolicy = shippingPolicy;
        this.memberTierService = memberTierService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listMyOrders(Long memberId, OrderStatus status, Pageable pageable) {
        Specification<Orders> spec = Specification.where(hasMemberId(memberId)).and(hasStatus(status));
        return orderRepository.findAll(spec, pageable).map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long memberId, Long orderId) {
        return OrderResponse.from(findOwnedOrThrow(memberId, orderId));
    }

    @Override
    public OrderResponse checkout(Long memberId, CheckoutRequest request) {
        Address address = addressRepository.findByIdAndMemberId(request.getAddressId(), memberId)
                .orElseThrow(() -> new BusinessException("收件地址不存在"));

        List<CartItem> cartItems = resolveCartItems(memberId, request.getCartItemIds());
        if (cartItems.isEmpty()) {
            throw new BusinessException("購物車是空的,無法結帳");
        }

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setMember(memberRepository.getReferenceById(memberId));
        order.setAddress(address);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentDeadline(paymentPolicy.deadlineFor(request.getPaymentMethod(), LocalDateTime.now()));
        order.markCreated(OrderActor.MEMBER);
        order.setReceiverName(address.getRecipientName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getCity() + address.getDistrict() + address.getDetailAddress());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            ProductSku sku = cartItem.getProductSku();
            Product product = sku.getProduct();

            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException("「" + product.getName() + "」已下架,請從購物車移除後再結帳");
            }
            if (sku.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("「" + product.getName() + " " + sku.getSpecName() + "」庫存不足");
            }

            // 下單當下的實際售價(含限時特價)寫進訂單明細當快照,活動結束後歷史訂單金額不變
            BigDecimal unitPrice = sku.getEffectivePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductSku(sku);
            orderItem.setProductName(product.getName());
            orderItem.setSpecName(sku.getSpecName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(subtotal);
            order.addItem(orderItem);

            // 上面的檢查只是為了給清楚的錯誤訊息;真正防超賣靠條件式 UPDATE(同時結帳時只有一人扣得到)
            if (productSkuRepository.decrementStock(sku.getId(), cartItem.getQuantity()) == 0) {
                throw new BusinessException("「" + product.getName() + " " + sku.getSpecName() + "」庫存不足");
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            CouponApplyResponse applied = couponService.reserve(couponCode.trim(), totalAmount, memberId);
            discountAmount = applied.getDiscountAmount();
            order.setCoupon(couponRepository.getReferenceById(applied.getCouponId()));
            order.setCouponCode(applied.getCode());
        }
        BigDecimal payable = totalAmount.subtract(discountAmount);
        int pointsToUse = request.getPointsToUse() == null ? 0 : request.getPointsToUse();
        if (pointsToUse > 0) {
            int balance = pointService.getBalance(memberId).getBalance();
            int limit = pointPolicy.maxRedeemable(balance, payable);
            if (pointsToUse > limit) {
                throw new BusinessException("本筆訂單最多可折抵 " + limit + " 點購物金");
            }
        }
        order.setSubtotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        BigDecimal shippingFee = shippingPolicy.feeFor(payable);
        order.setPointsUsed(pointsToUse);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(payable.subtract(BigDecimal.valueOf(pointsToUse)).add(shippingFee));

        Orders saved = orderRepository.save(order);
        if (pointsToUse > 0) {
            pointService.deduct(memberId, saved.getId(), pointsToUse, PointTransactionType.REDEEM,
                    "訂單 " + saved.getOrderNo() + " 折抵");
        }
        cartItemRepository.deleteAll(cartItems);
        orderNotifier.notifyStatusChanged(saved);

        return OrderResponse.from(saved);
    }

    @Override
    public OrderResponse pay(Long memberId, Long orderId) {
        Orders order = findOwnedOrThrow(memberId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("訂單狀態不正確,無法付款");
        }
        // 排程還沒掃到的逾期訂單也不能再付款
        if (order.getPaymentDeadline() != null && order.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("已超過付款期限,訂單將自動取消");
        }
        markPaid(order, OrderActor.MEMBER, "會員完成付款");
        orderNotifier.notifyStatusChanged(order);
        return OrderResponse.from(order);
    }

    @Override
    public OrderResponse cancelByMember(Long memberId, Long orderId) {
        Orders order = findOwnedOrThrow(memberId, orderId);
        cancelOrder(order, OrderActor.MEMBER, "會員取消訂單");
        orderNotifier.notifyStatusChanged(order);
        return OrderResponse.from(order);
    }

    @Override
    public ReorderResponse reorder(Long memberId, Long orderId) {
        Orders order = findOwnedOrThrow(memberId, orderId);
        int added = 0;
        List<String> notices = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            ProductSku sku = item.getProductSku();
            String label = item.getProductName() + " " + item.getSpecName();

            if (sku.getProduct().getStatus() != ProductStatus.ON_SALE) {
                notices.add("「" + label + "」已下架");
                continue;
            }

            CartItem cartItem = cartItemRepository.findByMemberIdAndProductSkuId(memberId, sku.getId())
                    .orElseGet(() -> {
                        CartItem newItem = new CartItem();
                        newItem.setMember(memberRepository.getReferenceById(memberId));
                        newItem.setProductSku(sku);
                        newItem.setQuantity(0);
                        return newItem;
                    });

            int addable = Math.min(item.getQuantity(), sku.getStock() - cartItem.getQuantity());
            if (addable <= 0) {
                notices.add("「" + label + "」庫存不足");
                continue;
            }
            if (addable < item.getQuantity()) {
                notices.add("「" + label + "」庫存不足,僅加入 " + addable + " 件");
            }
            cartItem.setQuantity(cartItem.getQuantity() + addable);
            cartItemRepository.save(cartItem);
            added++;
        }

        return new ReorderResponse(added, notices);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listAdmin(AdminOrderQuery query, Pageable pageable) {
        return orderRepository.findAll(adminSpec(query), pageable).map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAdminCsv(AdminOrderQuery query) {
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new BusinessException("開始日期不可晚於結束日期");
        }
        Pageable firstRows = PageRequest.of(0, MAX_EXPORT_ROWS, Sort.by(Sort.Direction.DESC, "createdAt"));
        return OrderCsvWriter.write(orderRepository.findAll(adminSpec(query), firstRows).getContent());
    }

    private static Specification<Orders> adminSpec(AdminOrderQuery query) {
        return Specification.where(hasStatus(query.getStatus()))
                .and(keywordMatches(query.getKeyword()))
                .and(createdBetween(query.getStartDate(), query.getEndDate()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getAdminOrder(Long orderId) {
        return OrderResponse.from(findOrThrow(orderId));
    }

    @Override
    public OrderResponse updateStatus(Long orderId, OrderStatusRequest request) {
        Orders order = findOrThrow(orderId);
        OrderStatus target = request.getStatus();

        if (!ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class)).contains(target)) {
            throw new BusinessException("不允許將訂單狀態從 " + order.getStatus() + " 變更為 " + target);
        }

        String note = blankToNull(request.getNote());
        if (target == OrderStatus.PAID) {
            markPaid(order, OrderActor.ADMIN, note);
        } else if (target == OrderStatus.CANCELLED) {
            cancelOrder(order, OrderActor.ADMIN, note);
        } else if (target == OrderStatus.SHIPPING) {
            markShipped(order, request, note);
        } else if (target == OrderStatus.COMPLETED) {
            // 等級以「這筆完成之前」的消費計算,避免這筆訂單自己把自己推上更高倍率
            MemberTier tier = memberTierService.tierOf(order.getMember().getId());
            order.changeStatus(target, OrderActor.ADMIN, note);
            // 回饋只算商品金額,運費不列入;依會員等級加倍
            int earned = pointPolicy.pointsEarnedFor(order.getTotalAmount().subtract(order.getShippingFee()),
                    tier.getPointsMultiplier());
            order.setPointsEarned(earned);
            if (earned > 0) {
                String tierNote = tier == MemberTier.NORMAL ? "" : "(" + tier.getLabel() + " "
                        + tier.getPointsMultiplier().stripTrailingZeros().toPlainString() + " 倍)";
                pointService.credit(order.getMember().getId(), order.getId(), earned, PointTransactionType.EARN,
                        "訂單 " + order.getOrderNo() + " 完成回饋" + tierNote);
            }
        } else {
            order.changeStatus(target, OrderActor.ADMIN, note);
        }
        orderNotifier.notifyStatusChanged(order);

        return OrderResponse.from(order);
    }

    @Override
    public int cancelExpiredOrders(LocalDateTime now) {
        List<Orders> expired = orderRepository.findByStatusAndPaymentDeadlineBefore(OrderStatus.PENDING_PAYMENT, now);
        for (Orders order : expired) {
            cancelOrder(order, OrderActor.SYSTEM, "逾時未付款,系統自動取消");
            orderNotifier.notifyStatusChanged(order);
        }
        return expired.size();
    }

    private void markPaid(Orders order, OrderActor actor, String note) {
        order.changeStatus(OrderStatus.PAID, actor, note);
        for (OrderItem item : order.getItems()) {
            productRepository.addSalesCount(item.getProductSku().getProduct().getId(), item.getQuantity());
        }
    }

    private void markShipped(Orders order, OrderStatusRequest request, String note) {
        String carrier = blankToNull(request.getShippingCarrier());
        String trackingNumber = blankToNull(request.getTrackingNumber());
        if (carrier == null || trackingNumber == null) {
            throw new BusinessException("出貨需填寫物流業者與物流單號");
        }
        order.setShippingCarrier(carrier);
        order.setTrackingNumber(trackingNumber);
        order.setShippedAt(LocalDateTime.now());
        order.changeStatus(OrderStatus.SHIPPING, OrderActor.ADMIN,
                note != null ? note : carrier + " " + trackingNumber);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void cancelOrder(Orders order, OrderActor actor, String note) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class))
                .contains(OrderStatus.CANCELLED)) {
            throw new BusinessException("此訂單狀態無法取消");
        }
        for (OrderItem item : order.getItems()) {
            productSkuRepository.incrementStock(item.getProductSku().getId(), item.getQuantity());
        }
        if (order.getCoupon() != null) {
            couponService.release(order.getCoupon().getId());
        }
        if (order.getPointsUsed() > 0) {
            pointService.credit(order.getMember().getId(), order.getId(), order.getPointsUsed(),
                    PointTransactionType.REFUND, "訂單 " + order.getOrderNo() + " 取消退還");
        }
        order.changeStatus(OrderStatus.CANCELLED, actor, note);
    }

    private List<CartItem> resolveCartItems(Long memberId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return cartItemRepository.findAllByMemberIdWithDetails(memberId);
        }
        return cartItemIds.stream()
                .map(id -> cartItemRepository.findByIdAndMemberId(id, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("購物車項目不存在:" + id)))
                .toList();
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timestamp + random;
    }

    private Orders findOwnedOrThrow(Long memberId, Long orderId) {
        return orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));
    }

    private Orders findOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));
    }
}
