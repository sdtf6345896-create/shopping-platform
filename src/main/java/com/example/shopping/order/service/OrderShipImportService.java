package com.example.shopping.order.service;

import com.example.shopping.common.csv.CsvImportReader;
import com.example.shopping.common.csv.CsvImportResponse;
import com.example.shopping.common.csv.CsvImportResponse.RowError;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.export.OrderCsvWriter;
import com.example.shopping.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批次出貨:以 CSV(訂單編號, 物流業者, 物流單號)把已付款訂單改為出貨中。
 * 先驗證全部資料列,有任何錯誤就一筆都不出貨;全部正確才逐筆出貨並通知會員。
 */
@Service
@Transactional
public class OrderShipImportService {

    static final int MAX_ROWS = 1000;
    static final long MAX_FILE_BYTES = 512 * 1024;
    static final int MAX_CARRIER_LENGTH = 30;
    static final int MAX_TRACKING_LENGTH = 50;

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderShipImportService(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /** 所有待出貨(已付款)訂單的匯入範本,填好物流欄位即可直接上傳 */
    @Transactional(readOnly = true)
    public byte[] template() {
        return OrderCsvWriter.writeShipTemplate(orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PAID));
    }

    public CsvImportResponse importCsv(MultipartFile file) {
        List<CsvImportReader.Row> csvRows =
                CsvImportReader.read(file, MAX_ROWS, MAX_FILE_BYTES, OrderShipImportService::isHeader);

        List<RowError> errors = new ArrayList<>();
        Map<String, ShipRow> rows = new LinkedHashMap<>();
        for (CsvImportReader.Row row : csvRows) {
            parseRow(row, rows, errors);
        }

        Map<String, Orders> orders = orderRepository.findByOrderNoIn(rows.keySet()).stream()
                .collect(Collectors.toMap(Orders::getOrderNo, Function.identity()));
        rows.forEach((orderNo, row) -> {
            Orders order = orders.get(orderNo);
            if (order == null) {
                errors.add(new RowError(row.line(), "找不到訂單:" + orderNo));
            } else if (order.getStatus() != OrderStatus.PAID) {
                errors.add(new RowError(row.line(), "訂單 " + orderNo + " 不是「已付款」狀態,無法出貨"));
            }
        });

        if (!errors.isEmpty()) {
            return CsvImportResponse.rejected(csvRows.size(), errors);
        }
        rows.forEach((orderNo, row) -> {
            OrderStatusRequest request = new OrderStatusRequest();
            request.setStatus(OrderStatus.SHIPPING);
            request.setShippingCarrier(row.carrier());
            request.setTrackingNumber(row.trackingNumber());
            orderService.updateStatus(orders.get(orderNo).getId(), request);
        });
        return new CsvImportResponse(true, csvRows.size(), rows.size(), List.of());
    }

    private record ShipRow(int line, String carrier, String trackingNumber) {
    }

    private static void parseRow(CsvImportReader.Row row, Map<String, ShipRow> rows, List<RowError> errors) {
        int lineNo = row.line();
        String orderNo = row.cell(0);
        String carrier = row.cell(1);
        String trackingNumber = row.cell(2);
        if (orderNo.isEmpty()) {
            errors.add(new RowError(lineNo, "訂單編號不可為空"));
            return;
        }
        if (carrier.isEmpty() || trackingNumber.isEmpty()) {
            errors.add(new RowError(lineNo, "訂單 " + orderNo + " 需填寫物流業者與物流單號"));
            return;
        }
        if (carrier.length() > MAX_CARRIER_LENGTH) {
            errors.add(new RowError(lineNo, "物流業者最多 " + MAX_CARRIER_LENGTH + " 字"));
            return;
        }
        if (trackingNumber.length() > MAX_TRACKING_LENGTH) {
            errors.add(new RowError(lineNo, "物流單號最多 " + MAX_TRACKING_LENGTH + " 字"));
            return;
        }
        ShipRow previous = rows.putIfAbsent(orderNo, new ShipRow(lineNo, carrier, trackingNumber));
        if (previous != null) {
            errors.add(new RowError(lineNo, "訂單重複(第 " + previous.line() + " 行已出現):" + orderNo));
        }
    }

    private static boolean isHeader(String line) {
        return line.contains("訂單") || line.toLowerCase().contains("order");
    }
}
