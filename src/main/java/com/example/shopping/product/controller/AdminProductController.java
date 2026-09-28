package com.example.shopping.product.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.csv.CsvImportResponse;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.request.BatchProductStatusRequest;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.LowStockSkuResponse;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import com.example.shopping.product.service.ProductService;
import com.example.shopping.product.service.StockImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final StockImportService stockImportService;

    public AdminProductController(ProductService productService, StockImportService stockImportService) {
        this.productService = productService;
        this.stockImportService = stockImportService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductListResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                productService.listAdmin(categoryId, status, keyword, pageable)));
    }

    @AdminAudit(action = "批次上下架", target = AuditTarget.PRODUCT, targetId = "",
            detail = "#request.status + ' ' + #request.ids")
    @PatchMapping("/status")
    public ApiResponse<Map<String, Integer>> updateStatusBatch(@Valid @RequestBody BatchProductStatusRequest request) {
        return ApiResponse.success("批次更新完成", Map.of("updated", productService.updateStatusBatch(request)));
    }

    @AdminAudit(action = "匯入庫存 CSV", target = AuditTarget.PRODUCT, targetId = "",
            detail = "#file.originalFilename + ' → ' + (#result?.data?.applied() ? '已更新 ' + #result.data.updated() + ' 筆' : '有錯誤未套用')")
    @PostMapping("/stock-import")
    public ApiResponse<CsvImportResponse> importStock(@RequestParam("file") MultipartFile file) {
        CsvImportResponse result = stockImportService.importCsv(file);
        return ApiResponse.success(result.applied() ? "庫存已更新" : "資料有誤,未更新任何庫存", result);
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<LowStockSkuResponse>> lowStock(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "50") int limit) {

        return ApiResponse.success(productService.listLowStock(threshold, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(productService.getAdminDetail(id));
    }

    @AdminAudit(action = "新增商品", target = AuditTarget.PRODUCT, detail = "#request.name")
    @PostMapping
    public ApiResponse<ProductDetailResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("新增成功", productService.create(request));
    }

    @AdminAudit(action = "修改商品", target = AuditTarget.PRODUCT, detail = "#request.name")
    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("更新成功", productService.update(id, request));
    }

    @AdminAudit(action = "商品上下架", target = AuditTarget.PRODUCT, detail = "#request.status")
    @PatchMapping("/{id}/status")
    public ApiResponse<ProductDetailResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody ProductStatusRequest request) {
        return ApiResponse.success("狀態更新成功", productService.updateStatus(id, request));
    }

    @AdminAudit(action = "修改庫存", target = AuditTarget.PRODUCT,
            targetId = "#productId",
            detail = "'SKU ' + #skuId + ' 庫存 → ' + #request.stock")
    @PatchMapping("/{productId}/skus/{skuId}/stock")
    public ApiResponse<SkuResponse> updateStock(@PathVariable Long productId,
                                                 @PathVariable Long skuId,
                                                 @Valid @RequestBody StockUpdateRequest request) {
        return ApiResponse.success("庫存更新成功", productService.updateStock(productId, skuId, request));
    }

    @AdminAudit(action = "刪除商品", target = AuditTarget.PRODUCT)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
