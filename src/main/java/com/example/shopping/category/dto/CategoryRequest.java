package com.example.shopping.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "分類名稱不可為空")
    private String name;

    private Long parentId;

    private int sortOrder;
}
