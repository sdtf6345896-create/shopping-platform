package com.example.shopping.banner.service;

import com.example.shopping.banner.dto.BannerRequest;
import com.example.shopping.banner.dto.BannerResponse;
import com.example.shopping.banner.dto.BannerStatusRequest;
import com.example.shopping.banner.entity.Banner;
import com.example.shopping.banner.repository.BannerRepository;
import com.example.shopping.common.enums.BannerStatus;
import com.example.shopping.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerServiceImpl bannerService;

    private Banner existingBanner;

    @BeforeEach
    void setUp() {
        existingBanner = new Banner();
        existingBanner.setId(1L);
        existingBanner.setTitle("新品上市");
        existingBanner.setSubtitle("本季新款搶先看");
        existingBanner.setImageUrl("https://placehold.co/1200x400");
        existingBanner.setLinkUrl("/products");
        existingBanner.setSortOrder(0);
        existingBanner.setStatus(BannerStatus.ACTIVE);
    }

    @Test
    void getActiveBanners_returnsOnlyActiveBanners() {
        when(bannerRepository.findByStatusOrderBySortOrderAscIdAsc(BannerStatus.ACTIVE))
                .thenReturn(List.of(existingBanner));

        List<BannerResponse> result = bannerService.getActiveBanners();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("新品上市");
        assertThat(result.get(0).getStatus()).isEqualTo(BannerStatus.ACTIVE);
    }

    @Test
    void listAll_returnsAllBanners() {
        Banner disabled = new Banner();
        disabled.setId(2L);
        disabled.setTitle("已下架");
        disabled.setImageUrl("https://placehold.co/1200x400");
        disabled.setStatus(BannerStatus.DISABLED);

        when(bannerRepository.findAllByOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(existingBanner, disabled));

        List<BannerResponse> result = bannerService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BannerResponse::getStatus)
                .containsExactly(BannerStatus.ACTIVE, BannerStatus.DISABLED);
    }

    @Test
    void create_savesBannerWithRequestFields() {
        when(bannerRepository.save(any(Banner.class))).thenAnswer(inv -> inv.getArgument(0));

        BannerRequest request = new BannerRequest();
        request.setTitle("限時優惠");
        request.setSubtitle("精選商品折扣中");
        request.setImageUrl("https://placehold.co/1200x400/blue");
        request.setLinkUrl("/products?sort=createdAt,desc");
        request.setSortOrder(1);

        BannerResponse response = bannerService.create(request);

        assertThat(response.getTitle()).isEqualTo("限時優惠");
        assertThat(response.getSubtitle()).isEqualTo("精選商品折扣中");
        assertThat(response.getImageUrl()).isEqualTo("https://placehold.co/1200x400/blue");
        assertThat(response.getLinkUrl()).isEqualTo("/products?sort=createdAt,desc");
        assertThat(response.getSortOrder()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(BannerStatus.ACTIVE);
    }

    @Test
    void update_appliesRequestFieldsToExistingBanner() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existingBanner));

        BannerRequest request = new BannerRequest();
        request.setTitle("改標題");
        request.setSubtitle("改副標題");
        request.setImageUrl("https://placehold.co/1200x400/new");
        request.setLinkUrl("/register");
        request.setSortOrder(5);

        BannerResponse response = bannerService.update(1L, request);

        assertThat(response.getTitle()).isEqualTo("改標題");
        assertThat(response.getSubtitle()).isEqualTo("改副標題");
        assertThat(response.getImageUrl()).isEqualTo("https://placehold.co/1200x400/new");
        assertThat(response.getLinkUrl()).isEqualTo("/register");
        assertThat(response.getSortOrder()).isEqualTo(5);
    }

    @Test
    void update_throws_whenBannerNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        BannerRequest request = new BannerRequest();
        request.setTitle("不存在");
        request.setImageUrl("https://placehold.co/1200x400");

        assertThatThrownBy(() -> bannerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_changesStatus() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existingBanner));
        BannerStatusRequest request = new BannerStatusRequest();
        request.setStatus(BannerStatus.DISABLED);

        BannerResponse response = bannerService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(BannerStatus.DISABLED);
    }

    @Test
    void updateStatus_throws_whenBannerNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());
        BannerStatusRequest request = new BannerStatusRequest();
        request.setStatus(BannerStatus.DISABLED);

        assertThatThrownBy(() -> bannerService.updateStatus(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesBanner_whenExists() {
        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existingBanner));

        bannerService.delete(1L);

        verify(bannerRepository).deleteById(1L);
    }

    @Test
    void delete_throws_whenBannerNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bannerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bannerRepository, never()).deleteById(any());
    }
}
