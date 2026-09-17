package com.example.shopping.banner.repository;

import com.example.shopping.banner.entity.Banner;
import com.example.shopping.common.enums.BannerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderBySortOrderAscIdAsc();

    List<Banner> findByStatusOrderBySortOrderAscIdAsc(BannerStatus status);
}
