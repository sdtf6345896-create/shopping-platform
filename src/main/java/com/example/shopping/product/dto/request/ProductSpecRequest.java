package com.example.shopping.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 規格表的一列 */
public record ProductSpecRequest(
        @NotBlank(message = "規格項目不可為空") @Size(max = 30, message = "規格項目最多 30 字") String name,
        @NotBlank(message = "規格內容不可為空") @Size(max = 200, message = "規格內容最多 200 字") String value) {
}
