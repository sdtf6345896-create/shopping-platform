package com.example.shopping.order.service;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.service.CouponService;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.shopping.order.repository.OrderSpecifications.hasMemberId;
import static com.example.shopping.order.repository.OrderSpecifications.hasStatus;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPING, EnumSet.of(OrderStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;
    private final CouponService couponService;
    private final CouponRepository couponRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                             CartItemRepository cartItemRepository,
                             AddressRepository addressRepository,
                             MemberRepository memberRepository,
                             CouponService couponService,
                             CouponRepository couponRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.memberRepository = memberRepository;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
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
        order.setStatus(OrderStatus.PENDING_PAYMENT);
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

            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductSku(sku);
            orderItem.setProductName(product.getName());
            orderItem.setSpecName(sku.getSpecName());
            orderItem.setUnitPrice(sku.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(subtotal);
            order.addItem(orderItem);

            sku.setStock(sku.getStock() - cartItem.getQuantity());
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            CouponApplyResponse applied = couponService.reserve(couponCode.trim(), totalAmount);
            discountAmount = applied.getDiscountAmount();
            order.setCoupon(couponRepository.getReferenceById(applied.getCouponId()));
            order.setCouponCode(applied.getCode());
        }
        order.setSubtotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount.subtract(discountAmount));

        Orders saved = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        return OrderResponse.from(saved);
    }

    @Override
    public OrderResponse pay(Long memberId, Long orderId) {
        Orders order = findOwnedOrThrow(memberId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("訂單狀態不正確,無法付款");
        }
        markPaid(order);
        return OrderResponse.from(order);
    }

    @Override
    public OrderResponse cancelByMember(Long memberId, Long orderId) {
        Orders order = findOwnedOrThrow(memberId, orderId);
        cancelOrder(order);
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listAdmin(OrderStatus status, Pageable pageable) {
        Specification<Orders> spec = Specification.where(hasStatus(status));
        return orderRepository.findAll(spec, pageable).map(OrderResponse::from);
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

        if (target == OrderStatus.PAID) {
            markPaid(order);
        } else if (target == OrderStatus.CANCELLED) {
            cancelOrder(order);
        } else {
            order.setStatus(target);
        }

        return OrderResponse.from(order);
    }

    private void markPaid(Orders order) {
        order.setStatus(OrderStatus.PAID);
        for (OrderItem item : order.getItems()) {
            Product product = item.getProductSku().getProduct();
            product.setSalesCount(product.getSalesCount() + item.getQuantity());
        }
    }

    private void cancelOrder(Orders order) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class))
                .contains(OrderStatus.CANCELLED)) {
            throw new BusinessException("此訂單狀態無法取消");
        }
        for (OrderItem item : order.getItems()) {
            ProductSku sku = item.getProductSku();
            sku.setStock(sku.getStock() + item.getQuantity());
        }
        if (order.getCoupon() != null) {
            couponService.release(order.getCoupon().getId());
        }
        order.setStatus(OrderStatus.CANCELLED);
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
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
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
