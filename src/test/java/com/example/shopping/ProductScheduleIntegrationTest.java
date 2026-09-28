package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.service.ProductScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** 到期的排程上架 / 下架會套用並清空;未到期的不動;兩者都過期時結果為下架 */
@SpringBootTest
@ActiveProfiles("test")
class ProductScheduleIntegrationTest {

    @Autowired
    private ProductScheduleService scheduleService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private Product product(ProductStatus status, LocalDateTime publishAt, LocalDateTime unpublishAt) {
        Category category = new Category();
        category.setName("排程測試");
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("排程商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(status);
        product.setPublishAt(publishAt);
        product.setUnpublishAt(unpublishAt);
        return productRepository.save(product);
    }

    private Product reload(Product product) {
        return productRepository.findById(product.getId()).orElseThrow();
    }

    @Test
    void appliesDueSchedulesOnly() {
        LocalDateTime now = LocalDateTime.of(2031, 3, 1, 10, 0);
        Product launching = product(ProductStatus.OFF_SHELF, now.minusMinutes(1), now.plusDays(7));
        Product later = product(ProductStatus.OFF_SHELF, now.plusMinutes(5), null);
        Product ending = product(ProductStatus.ON_SALE, null, now);
        Product missedBoth = product(ProductStatus.OFF_SHELF, now.minusDays(2), now.minusDays(1));

        scheduleService.applyDue(now);

        assertThat(reload(launching).getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(reload(launching).getPublishAt()).isNull();
        assertThat(reload(launching).getUnpublishAt()).isEqualTo(now.plusDays(7));
        assertThat(reload(later).getStatus()).isEqualTo(ProductStatus.OFF_SHELF);
        assertThat(reload(later).getPublishAt()).isEqualTo(now.plusMinutes(5));
        assertThat(reload(ending).getStatus()).isEqualTo(ProductStatus.OFF_SHELF);
        assertThat(reload(ending).getUnpublishAt()).isNull();
        assertThat(reload(missedBoth).getStatus()).isEqualTo(ProductStatus.OFF_SHELF);
        assertThat(reload(missedBoth).getPublishAt()).isNull();
    }
}
