package com.example.shopping.banner.dto;

import com.example.shopping.common.enums.BannerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerStatusRequest {

    @NotNull(message = "狀態不可為空")
    private BannerStatus status;
}
