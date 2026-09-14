package com.example.shopping.product.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import com.example.shopping.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(productService.getAdminDetail(id));
    }

    @PostMapping
    public ApiResponse<ProductDetailResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("新增成功", productService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("更新成功", productService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ProductDetailResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody ProductStatusRequest request) {
        return ApiResponse.success("狀態更新成功", productService.updateStatus(id, request));
    }

    @PatchMapping("/{productId}/skus/{skuId}/stock")
    public ApiResponse<SkuResponse> updateStock(@PathVariable Long productId,
                                                 @PathVariable Long skuId,
                                                 @Valid @RequestBody StockUpdateRequest request) {
        return ApiResponse.success("庫存更新成功", productService.updateStock(productId, skuId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
