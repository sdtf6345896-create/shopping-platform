package com.example.shopping.product.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.LowStockSkuResponse;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import com.example.shopping.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
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
