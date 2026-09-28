package com.example.shopping.promotion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PromotionRequest {

    @NotBlank(message = "活動名稱不可為空")
    @Size(max = 50, message = "活動名稱最多 50 字")
    private String name;

    /** 適用分類(含子分類),不填為全站 */
    private Long categoryId;

    @Min(value = 2, message = "滿件數量至少 2 件")
    @Max(value = 99, message = "滿件數量最多 99 件")
    private int minQuantity;

    @Min(value = 1, message = "折扣至少 1%")
    @Max(value = 90, message = "折扣最多 90%")
    private int discountPercent;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private boolean active = true;
}
