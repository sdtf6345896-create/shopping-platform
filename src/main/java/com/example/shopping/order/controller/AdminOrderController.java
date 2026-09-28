package com.example.shopping.order.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.csv.CsvImportResponse;
import com.example.shopping.order.dto.request.AdminOrderQuery;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.service.OrderService;
import com.example.shopping.order.service.OrderShipImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderShipImportService orderShipImportService;

    public AdminOrderController(OrderService orderService, OrderShipImportService orderShipImportService) {
        this.orderService = orderService;
        this.orderShipImportService = orderShipImportService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            AdminOrderQuery query,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ApiResponse.success(PageResponse.from(orderService.listAdmin(query, pageable)));
    }

    @AdminAudit(action = "匯出訂單 CSV", target = AuditTarget.ORDER,
            targetId = "",
            detail = "'status=' + #query.status + ', keyword=' + #query.keyword"
                    + " + ', ' + #query.startDate + '~' + #query.endDate")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(AdminOrderQuery query) {
        String filename = "orders-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(orderService.exportAdminCsv(query));
    }

    @GetMapping("/ship-template")
    public ResponseEntity<byte[]> shipTemplate() {
        String filename = "ship-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(orderShipImportService.template());
    }

    @AdminAudit(action = "匯入出貨單號 CSV", target = AuditTarget.ORDER, targetId = "",
            detail = "#file.originalFilename + ' → ' + (#result?.data?.applied() ? '已出貨 ' + #result.data.updated() + ' 筆' : '有錯誤未套用')")
    @PostMapping("/ship-import")
    public ApiResponse<CsvImportResponse> importShipments(@RequestParam("file") MultipartFile file) {
        CsvImportResponse result = orderShipImportService.importCsv(file);
        return ApiResponse.success(result.applied() ? "已批次出貨 " + result.updated() + " 筆" : "資料有誤,未出貨任何訂單", result);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.getAdminOrder(id));
    }

    @AdminAudit(action = "更新訂單狀態", target = AuditTarget.ORDER,
            detail = "(#request.status + ' ' + (#request.shippingCarrier ?: '') + ' ' + (#request.trackingNumber ?: '')"
                    + " + ' ' + (#request.note ?: '')).trim()")
    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody OrderStatusRequest request) {
        return ApiResponse.success("狀態更新成功", orderService.updateStatus(id, request));
    }
}
