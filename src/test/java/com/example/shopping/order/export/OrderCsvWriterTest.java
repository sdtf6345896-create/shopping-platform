package com.example.shopping.order.export;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.member.entity.Member;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCsvWriterTest {

    @Test
    void write_startsWithBomAndHeader_thenOneLinePerOrder() {
        Member member = new Member();
        member.setEmail("member@example.com");

        Orders order = new Orders();
        order.setMember(member);
        order.setOrderNo("ORD001");
        order.setCreatedAt(LocalDateTime.of(2026, 9, 27, 15, 30, 0));
        order.setStatus(OrderStatus.SHIPPING);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setReceiverName("王小明");
        order.setReceiverPhone("0912345678");
        order.setReceiverAddress("台北市大安區復興南路一段1號");
        order.setSubtotalAmount(new BigDecimal("1180.00"));
        order.setDiscountAmount(new BigDecimal("100.00"));
        order.setTotalAmount(new BigDecimal("1080.00"));
        order.setCouponCode("SAVE100");
        order.setShippingCarrier("黑貓宅急便");
        order.setTrackingNumber("TRK123");
        order.setBuyerNote("請於平日配送,謝謝");
        OrderItem item = new OrderItem();
        item.setProductName("經典圓領T恤");
        item.setSpecName("黑色/M");
        item.setQuantity(2);
        order.addItem(item);

        byte[] bytes = OrderCsvWriter.write(List.of(order));

        assertThat(Arrays.copyOf(bytes, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        String[] lines = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8).split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).startsWith("訂單編號,建立時間,狀態");
        assertThat(lines[1]).isEqualTo("ORD001,2026-09-27 15:30:00,出貨中,信用卡,member@example.com,宅配,王小明,0912345678,"
                + "台北市大安區復興南路一段1號,經典圓領T恤 黑色/M x2,1180.00,100.00,SAVE100,0,0,1080.00,黑貓宅急便,TRK123,\"請於平日配送,謝謝\",會員載具");
    }

    @Test
    void escape_quotesCommasQuotesAndNewlines() {
        assertThat(OrderCsvWriter.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(OrderCsvWriter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(OrderCsvWriter.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(OrderCsvWriter.escape(null)).isEmpty();
    }

    @Test
    void escape_neutralizesFormulaInjection_butKeepsNegativeNumbers() {
        assertThat(OrderCsvWriter.escape("=HYPERLINK(\"http://evil\")")).startsWith("\"'=HYPERLINK");
        assertThat(OrderCsvWriter.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(OrderCsvWriter.escape("-100.00")).isEqualTo("-100.00");
    }
}
