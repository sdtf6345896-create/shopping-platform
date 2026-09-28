package com.example.shopping.promotion;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionActiveRequest {

    @NotNull(message = "請指定是否啟用")
    private Boolean active;
}
