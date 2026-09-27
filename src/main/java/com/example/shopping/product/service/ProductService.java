package com.example.shopping.product.service;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.LowStockSkuResponse;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    Page<ProductListResponse> listPublic(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                          String keyword, Pageable pageable);

    Page<ProductListResponse> listAdmin(Long categoryId, ProductStatus status, String keyword, Pageable pageable);

    ProductDetailResponse getPublicDetail(Long id);

    ProductDetailResponse getAdminDetail(Long id);

    /** 相關商品:同分類熱銷優先,不足時以全站熱銷補齊 */
    List<ProductListResponse> listRelated(Long productId, int limit);

    /** 目前限時特價中的上架商品,最快結束的排前面 */
    List<ProductListResponse> listFlashSale(int limit);

    ProductDetailResponse create(ProductRequest request);

    ProductDetailResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductDetailResponse updateStatus(Long id, ProductStatusRequest request);

    SkuResponse updateStock(Long productId, Long skuId, StockUpdateRequest request);

    /** 上架中商品裡庫存小於等於 threshold 的規格(最多 limit 筆) */
    List<LowStockSkuResponse> listLowStock(int threshold, int limit);
}
