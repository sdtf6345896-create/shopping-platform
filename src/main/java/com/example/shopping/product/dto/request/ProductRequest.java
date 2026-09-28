package com.example.shopping.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotNull(message = "分類不可為空")
    private Long categoryId;

    @NotBlank(message = "商品名稱不可為空")
    private String name;

    private String description;

    @NotNull(message = "價格不可為空")
    @DecimalMin(value = "0", inclusive = true, message = "價格不可為負數")
    private BigDecimal price;

    private String mainImage;

    /** 主圖以外的商品圖片網址(依順序顯示),最多 8 張 */
    @Size(max = 8, message = "商品圖片最多 8 張")
    private List<@NotBlank(message = "圖片網址不可為空") @Size(max = 500, message = "圖片網址過長") String> images;

    /** 限時特價折扣百分比(1~90),不設定特價則留空 */
    @Min(value = 1, message = "折扣至少 1%")
    @Max(value = 90, message = "折扣最多 90%")
    private Integer saleDiscountPercent;

    private LocalDateTime saleStartAt;

    private LocalDateTime saleEndAt;

    /** 排程上架時間,選填 */
    private LocalDateTime publishAt;

    /** 排程下架時間,選填 */
    private LocalDateTime unpublishAt;

    @NotEmpty(message = "至少需要一個規格(SKU)")
    @Valid
    private List<SkuRequest> skus;
}
