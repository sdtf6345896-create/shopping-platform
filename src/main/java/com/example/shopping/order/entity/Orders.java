package com.example.shopping.order.entity;

import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.returns.entity.ReturnRequest;
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

    /** 運費(未達免運門檻時收取) */
    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /** 實付金額(= subtotalAmount - discountAmount - pointsUsed + shippingFee) */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    /** 使用的購物金點數(1 點 = NT$1),已從 totalAmount 扣除 */
    @Column(name = "points_used", nullable = false)
    private int pointsUsed;

    /** 訂單完成時回饋的購物金點數,退貨時據此收回 */
    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    /**
     * 退貨申請(資料庫 unique 限制一筆訂單最多一筆)。刻意用 OneToMany 對應:
     * 反向的 OneToOne 無法延遲/批次載入,訂單列表會變成每筆訂單各查一次(N+1)。
     */
    @OneToMany(mappedBy = "order")
    private List<ReturnRequest> returnRequests = new ArrayList<>();

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

    /** 會員結帳時留的備註 */
    @Column(name = "buyer_note", length = 200)
    private String buyerNote;

    /** 物流業者(出貨時填寫) */
    @Column(name = "shipping_carrier", length = 30)
    private String shippingCarrier;

    /** 物流追蹤單號(出貨時填寫) */
    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /** 付款期限,逾期未付款會被排程自動取消;貨到付款為 null */
    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

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

    public ReturnRequest getReturnRequest() {
        return returnRequests.isEmpty() ? null : returnRequests.get(0);
    }

    public void setReturnRequest(ReturnRequest returnRequest) {
        returnRequests.clear();
        if (returnRequest != null) {
            returnRequests.add(returnRequest);
        }
    }

    /** 最後一次變成 COMPLETED 的時間,沒有完成過回傳 null */
    public LocalDateTime getCompletedAt() {
        LocalDateTime completedAt = null;
        for (OrderStatusLog log : statusLogs) {
            if (log.getToStatus() == OrderStatus.COMPLETED) {
                completedAt = log.getCreatedAt();
            }
        }
        return completedAt;
    }

    /** 可申請退貨的期限(完成時間 + 鑑賞期);非已完成訂單回傳 null */
    public LocalDateTime getReturnDeadline() {
        LocalDateTime completedAt = getCompletedAt();
        return status == OrderStatus.COMPLETED && completedAt != null
                ? completedAt.plus(ReturnRequest.RETURN_WINDOW)
                : null;
    }

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
