package com.example.shopping.product.dto.response;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String mainImage;
    /** 主圖以外的商品圖片網址 */
    private List<String> images;
    private ProductStatus status;
    private int salesCount;
    private List<SkuResponse> skus;

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getMainImage(),
                product.getImages().stream().map(ProductImage::getUrl).toList(),
                product.getStatus(),
                product.getSalesCount(),
                product.getSkus().stream().map(SkuResponse::from).toList());
    }
}
