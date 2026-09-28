package com.example.shopping.product.service;

import com.example.shopping.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 套用到期的商品排程。先上架再下架:兩個時間都已過(例如停機期間錯過)時,結果是下架。
 * 用整批條件式 UPDATE,多台機器同時跑也只是重複套用同樣結果。
 */
@Service
@Transactional
public class ProductScheduleService {

    private final ProductRepository productRepository;

    public ProductScheduleService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public record Result(int published, int unpublished) {
    }

    public Result applyDue(LocalDateTime now) {
        int published = productRepository.publishDue(now);
        int unpublished = productRepository.unpublishDue(now);
        return new Result(published, unpublished);
    }
}
