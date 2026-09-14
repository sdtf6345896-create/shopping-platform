package com.example.shopping.category.dto;

import com.example.shopping.common.enums.CategoryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryStatusRequest {

    @NotNull(message = "狀態不可為空")
    private CategoryStatus status;
}
