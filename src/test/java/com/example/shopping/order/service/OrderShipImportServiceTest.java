package com.example.shopping.order.service;

import com.example.shopping.common.csv.CsvImportResponse;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderShipImportServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;

    private OrderShipImportService service;
    private Orders paidA;
    private Orders paidB;
    private Orders pending;

    @BeforeEach
    void setUp() {
        service = new OrderShipImportService(orderRepository, orderService);
        paidA = order(1L, "SO0001", OrderStatus.PAID);
        paidB = order(2L, "SO0002", OrderStatus.PAID);
        pending = order(3L, "SO0003", OrderStatus.PENDING_PAYMENT);
    }

    private static Orders order(Long id, String orderNo, OrderStatus status) {
        Orders order = new Orders();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setStatus(status);
        return order;
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "ship.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importCsv_shipsEveryRow_readingOnlyFirstThreeColumnsOfTemplate() {
        when(orderRepository.findByOrderNoIn(any())).thenReturn(List.of(paidA, paidB));

        CsvImportResponse result = service.importCsv(csv("""
                訂單編號,物流業者,物流單號,收件人,收件電話,收件地址,商品明細,訂單備註
                SO0001,黑貓宅急便,900100200300,王小明,0912345678,"台北市信義區, 10 樓",T恤 x1,
                SO0002,新竹物流, 55667788 ,李小華,0987654321,台中市,水壺 x2,"請盡快"
                """));

        assertThat(result.applied()).isTrue();
        assertThat(result.updated()).isEqualTo(2);
        ArgumentCaptor<OrderStatusRequest> request = ArgumentCaptor.forClass(OrderStatusRequest.class);
        verify(orderService).updateStatus(eq(2L), request.capture());
        assertThat(request.getValue().getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(request.getValue().getShippingCarrier()).isEqualTo("新竹物流");
        assertThat(request.getValue().getTrackingNumber()).isEqualTo("55667788");
        verify(orderService).updateStatus(eq(1L), any());
    }

    @Test
    void importCsv_shipsNothing_whenAnyRowIsInvalid() {
        when(orderRepository.findByOrderNoIn(any())).thenReturn(List.of(paidA, paidB, pending));

        CsvImportResponse result = service.importCsv(csv("""
                SO0001,黑貓宅急便,900100200300
                SO0003,黑貓宅急便,111
                SO9999,黑貓宅急便,222
                SO0002,新竹物流,
                SO0001,新竹物流,333
                """));

        assertThat(result.applied()).isFalse();
        assertThat(result.totalRows()).isEqualTo(5);
        assertThat(result.errors()).extracting(CsvImportResponse.RowError::line).containsExactly(2, 3, 4, 5);
        assertThat(result.errors().get(0).message()).contains("不是「已付款」");
        assertThat(result.errors().get(1).message()).contains("找不到訂單");
        assertThat(result.errors().get(2).message()).contains("物流業者與物流單號");
        assertThat(result.errors().get(3).message()).contains("重複");
        verify(orderService, never()).updateStatus(anyLong(), any());
    }
}
