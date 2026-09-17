package com.example.shopping.banner.dto;

import com.example.shopping.banner.entity.Banner;
import com.example.shopping.common.enums.BannerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
    private int sortOrder;
    private BannerStatus status;

    public static BannerResponse from(Banner banner) {
        BannerResponse response = new BannerResponse();
        response.setId(banner.getId());
        response.setTitle(banner.getTitle());
        response.setSubtitle(banner.getSubtitle());
        response.setImageUrl(banner.getImageUrl());
        response.setLinkUrl(banner.getLinkUrl());
        response.setSortOrder(banner.getSortOrder());
        response.setStatus(banner.getStatus());
        return response;
    }
}
