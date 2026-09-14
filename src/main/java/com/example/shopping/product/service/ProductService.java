package com.example.shopping.product.service;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    Page<ProductListResponse> listPublic(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                          String keyword, Pageable pageable);

    Page<ProductListResponse> listAdmin(Long categoryId, ProductStatus status, String keyword, Pageable pageable);

    ProductDetailResponse getPublicDetail(Long id);

    ProductDetailResponse getAdminDetail(Long id);

    ProductDetailResponse create(ProductRequest request);

    ProductDetailResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductDetailResponse updateStatus(Long id, ProductStatusRequest request);

    SkuResponse updateStock(Long productId, Long skuId, StockUpdateRequest request);
}
