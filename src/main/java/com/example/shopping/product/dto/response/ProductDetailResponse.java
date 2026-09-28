package com.example.shopping.product.dto.response;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    /** 限時特價價格,不在特價期間為 null */
    private BigDecimal salePrice;
    /** 限時特價設定(後台編輯用,前台可用 salePrice 是否為 null 判斷是否特價中) */
    private Integer saleDiscountPercent;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
    private LocalDateTime publishAt;
    private LocalDateTime unpublishAt;
    private String mainImage;
    /** 主圖以外的商品圖片網址 */
    private List<String> images;
    /** 規格表 */
    private List<SpecRow> specs;
    private ProductStatus status;
    private int salesCount;
    private BigDecimal ratingAverage;
    private int reviewCount;
    private List<SkuResponse> skus;

    public record SpecRow(String name, String value) {
    }

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isOnSale() ? product.applySale(product.getPrice()) : null,
                product.getSaleDiscountPercent(),
                product.getSaleStartAt(),
                product.getSaleEndAt(),
                product.getPublishAt(),
                product.getUnpublishAt(),
                product.getMainImage(),
                product.getImages().stream().map(ProductImage::getUrl).toList(),
                product.getSpecs().stream().map(s -> new SpecRow(s.getName(), s.getValue())).toList(),
                product.getStatus(),
                product.getSalesCount(),
                product.getRatingAverage(),
                product.getReviewCount(),
                product.getSkus().stream().map(SkuResponse::from).toList());
    }
}
