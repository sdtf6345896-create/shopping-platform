package com.example.shopping.recommendation.dto;

import com.example.shopping.product.dto.response.ProductListResponse;

/** @param orderCount 一起出現在幾筆訂單中 */
public record BoughtTogetherResponse(ProductListResponse product, long orderCount) {
}
