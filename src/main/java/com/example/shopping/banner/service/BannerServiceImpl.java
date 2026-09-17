package com.example.shopping.banner.service;

import com.example.shopping.banner.dto.BannerRequest;
import com.example.shopping.banner.dto.BannerResponse;
import com.example.shopping.banner.dto.BannerStatusRequest;
import com.example.shopping.banner.entity.Banner;
import com.example.shopping.banner.repository.BannerRepository;
import com.example.shopping.common.enums.BannerStatus;
import com.example.shopping.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    public BannerServiceImpl(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByStatusOrderBySortOrderAscIdAsc(BannerStatus.ACTIVE).stream()
                .map(BannerResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listAll() {
        return bannerRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(BannerResponse::from)
                .toList();
    }

    @Override
    public BannerResponse create(BannerRequest request) {
        Banner banner = new Banner();
        applyRequest(banner, request);
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Override
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = findOrThrow(id);
        applyRequest(banner, request);
        return BannerResponse.from(banner);
    }

    @Override
    public BannerResponse updateStatus(Long id, BannerStatusRequest request) {
        Banner banner = findOrThrow(id);
        banner.setStatus(request.getStatus());
        return BannerResponse.from(banner);
    }

    @Override
    public void delete(Long id) {
        findOrThrow(id);
        bannerRepository.deleteById(id);
    }

    private void applyRequest(Banner banner, BannerRequest request) {
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setSortOrder(request.getSortOrder());
    }

    private Banner findOrThrow(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner 不存在"));
    }
}
