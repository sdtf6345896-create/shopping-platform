package com.example.shopping.order.entity;

import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    /** 商品原價小計(套用優惠券前) */
    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    /** 優惠券折抵金額 */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** 實付金額(= subtotalAmount - discountAmount) */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false, length = 255)
    private String receiverAddress;

    /** 物流業者(出貨時填寫) */
    @Column(name = "shipping_carrier", length = 30)
    private String shippingCarrier;

    /** 物流追蹤單號(出貨時填寫) */
    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderStatusLog> statusLogs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /** 訂單建立時寫入第一筆歷程紀錄(沒有前一個狀態) */
    public void markCreated(OrderActor actor) {
        this.status = OrderStatus.PENDING_PAYMENT;
        appendLog(null, OrderStatus.PENDING_PAYMENT, actor, "訂單成立");
    }

    /** 變更訂單狀態並寫入一筆歷程紀錄 */
    public void changeStatus(OrderStatus target, OrderActor actor, String note) {
        appendLog(this.status, target, actor, note);
        this.status = target;
    }

    private void appendLog(OrderStatus from, OrderStatus to, OrderActor actor, String note) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrder(this);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setActor(actor);
        log.setNote(note);
        statusLogs.add(log);
    }
}
