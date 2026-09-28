package com.example.shopping.order.export;

import com.example.shopping.common.csv.CsvWriter;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ShippingMethod;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.invoice.InvoiceInfo;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 把訂單轉成 CSV(格式與跳脫規則見 {@link CsvWriter})。
 */
public final class OrderCsvWriter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<OrderStatus, String> STATUS_LABELS = new EnumMap<>(Map.of(
            OrderStatus.PENDING_PAYMENT, "待付款",
            OrderStatus.PAID, "已付款",
            OrderStatus.SHIPPING, "出貨中",
            OrderStatus.COMPLETED, "已完成",
            OrderStatus.CANCELLED, "已取消",
            OrderStatus.REFUNDED, "已退款"));

    private static final Map<PaymentMethod, String> PAYMENT_LABELS = new EnumMap<>(Map.of(
            PaymentMethod.CREDIT_CARD, "信用卡",
            PaymentMethod.ATM, "ATM 轉帳",
            PaymentMethod.COD, "貨到付款"));

    private static final List<String> HEADERS = List.of(
            "訂單編號", "建立時間", "狀態", "付款方式", "會員 Email", "配送方式", "收件人", "收件電話", "收件地址",
            "商品明細", "商品小計", "滿件折扣", "折抵金額", "優惠券", "購物金折抵", "運費", "實付金額", "物流業者", "物流單號", "訂單備註", "發票");

    /** 出貨單號匯入範本:前三欄是匯入要讀的欄位,其餘欄位只是方便對照,匯入時忽略 */
    public static final List<String> SHIP_TEMPLATE_HEADERS = List.of(
            "訂單編號", "物流業者", "物流單號", "收件人", "收件電話", "收件地址", "商品明細", "訂單備註");

    private OrderCsvWriter() {
    }

    public static byte[] writeShipTemplate(List<Orders> orders) {
        List<List<String>> rows = new ArrayList<>();
        for (Orders order : orders) {
            rows.add(List.of(
                    order.getOrderNo(),
                    "",
                    "",
                    order.getReceiverName(),
                    order.getReceiverPhone(),
                    order.getReceiverAddress(),
                    order.getItems().stream().map(OrderCsvWriter::describeItem).collect(Collectors.joining("; ")),
                    nullToEmpty(order.getBuyerNote())));
        }
        return CsvWriter.write(SHIP_TEMPLATE_HEADERS, rows);
    }

    public static byte[] write(List<Orders> orders) {
        List<List<String>> rows = new ArrayList<>();
        for (Orders order : orders) {
            rows.add(List.of(
                    order.getOrderNo(),
                    order.getCreatedAt() == null ? "" : order.getCreatedAt().format(DATE_TIME),
                    STATUS_LABELS.get(order.getStatus()),
                    PAYMENT_LABELS.get(order.getPaymentMethod()),
                    order.getMember().getEmail(),
                    order.getShippingMethod() == ShippingMethod.CVS_PICKUP ? "超商取貨" : "宅配",
                    order.getReceiverName(),
                    order.getReceiverPhone(),
                    order.getReceiverAddress(),
                    order.getItems().stream().map(OrderCsvWriter::describeItem).collect(Collectors.joining("; ")),
                    String.valueOf(order.getSubtotalAmount()),
                    String.valueOf(order.getPromotionDiscount()),
                    String.valueOf(order.getDiscountAmount()),
                    nullToEmpty(order.getCouponCode()),
                    String.valueOf(order.getPointsUsed()),
                    String.valueOf(order.getShippingFee()),
                    String.valueOf(order.getTotalAmount()),
                    nullToEmpty(order.getShippingCarrier()),
                    nullToEmpty(order.getTrackingNumber()),
                    nullToEmpty(order.getBuyerNote()),
                    describeInvoice(order.getInvoice())));
        }
        return CsvWriter.write(HEADERS, rows);
    }

    private static String describeInvoice(InvoiceInfo invoice) {
        if (invoice == null || invoice.getType() == null) {
            return "會員載具";
        }
        return switch (invoice.getType()) {
            case MEMBER_CARRIER -> "會員載具";
            case MOBILE_BARCODE -> "手機條碼 " + invoice.getCarrierCode();
            case COMPANY -> "統編 " + invoice.getTaxId() + " " + invoice.getCompanyTitle();
            case DONATION -> "捐贈 " + invoice.getDonationCode();
        };
    }

    private static String describeItem(OrderItem item) {
        return item.getProductName() + " " + item.getSpecName() + " x" + item.getQuantity();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
