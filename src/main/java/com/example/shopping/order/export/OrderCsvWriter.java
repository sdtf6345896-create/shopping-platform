package com.example.shopping.order.export;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.invoice.InvoiceInfo;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 把訂單轉成 CSV。開頭加 UTF-8 BOM,Excel 直接開啟中文才不會變亂碼。
 */
public final class OrderCsvWriter {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
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
            "訂單編號", "建立時間", "狀態", "付款方式", "會員 Email", "收件人", "收件電話", "收件地址",
            "商品明細", "商品小計", "折抵金額", "優惠券", "購物金折抵", "運費", "實付金額", "物流業者", "物流單號", "訂單備註", "發票");

    private OrderCsvWriter() {
    }

    public static byte[] write(List<Orders> orders) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, HEADERS);
        for (Orders order : orders) {
            appendRow(sb, List.of(
                    order.getOrderNo(),
                    order.getCreatedAt() == null ? "" : order.getCreatedAt().format(DATE_TIME),
                    STATUS_LABELS.get(order.getStatus()),
                    PAYMENT_LABELS.get(order.getPaymentMethod()),
                    order.getMember().getEmail(),
                    order.getReceiverName(),
                    order.getReceiverPhone(),
                    order.getReceiverAddress(),
                    order.getItems().stream().map(OrderCsvWriter::describeItem).collect(Collectors.joining("; ")),
                    String.valueOf(order.getSubtotalAmount()),
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

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, result, UTF8_BOM.length, body.length);
        return result;
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

    private static void appendRow(StringBuilder sb, List<String> cells) {
        sb.append(cells.stream().map(OrderCsvWriter::escape).collect(Collectors.joining(","))).append("\r\n");
    }

    /**
     * RFC 4180 跳脫;另外對 = + - @ 開頭的值加上單引號,避免 Excel 當成公式執行(CSV injection)。
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0 && !isNumber(safe)) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static boolean isNumber(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
