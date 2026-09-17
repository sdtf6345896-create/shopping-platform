package com.example.shopping.banner.service;

import com.example.shopping.banner.dto.BannerRequest;
import com.example.shopping.banner.dto.BannerResponse;
import com.example.shopping.banner.dto.BannerStatusRequest;

import java.util.List;

public interface BannerService {

    List<BannerResponse> getActiveBanners();

    List<BannerResponse> listAll();

    BannerResponse create(BannerRequest request);

    BannerResponse update(Long id, BannerRequest request);

    BannerResponse updateStatus(Long id, BannerStatusRequest request);

    void delete(Long id);
}
