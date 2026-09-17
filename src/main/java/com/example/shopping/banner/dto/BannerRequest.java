package com.example.shopping.banner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerRequest {

    @NotBlank(message = "標題不可為空")
    private String title;

    private String subtitle;

    @NotBlank(message = "圖片網址不可為空")
    private String imageUrl;

    private String linkUrl;

    private int sortOrder;
}
