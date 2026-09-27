package com.example.shopping.product.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSaleTest {

    private Product productOnSale(int percent, LocalDateTime start, LocalDateTime end) {
        Product product = new Product();
        product.setSaleDiscountPercent(percent);
        product.setSaleStartAt(start);
        product.setSaleEndAt(end);
        return product;
    }

    @Test
    void applySale_discountsAndRoundsToWholeDollars_duringSale() {
        LocalDateTime now = LocalDateTime.now();
        Product product = productOnSale(15, now.minusHours(1), now.plusHours(1));

        assertThat(product.isOnSale()).isTrue();
        // 590 × 85% = 501.5 → 502
        assertThat(product.applySale(new BigDecimal("590.00"))).isEqualByComparingTo("502.00");
    }

    @Test
    void applySale_returnsOriginalPrice_outsideSaleWindow() {
        LocalDateTime now = LocalDateTime.now();

        Product upcoming = productOnSale(20, now.plusHours(1), now.plusHours(2));
        Product ended = productOnSale(20, now.minusHours(2), now.minusSeconds(1));

        assertThat(upcoming.isOnSale()).isFalse();
        assertThat(upcoming.applySale(new BigDecimal("500"))).isEqualByComparingTo("500");
        assertThat(ended.isOnSale()).isFalse();
        assertThat(ended.applySale(new BigDecimal("500"))).isEqualByComparingTo("500");
    }

    @Test
    void applySale_returnsOriginalPrice_whenNoSaleConfigured() {
        Product product = new Product();

        assertThat(product.isOnSale()).isFalse();
        assertThat(product.applySale(new BigDecimal("500"))).isEqualByComparingTo("500");
    }

    @Test
    void skuEffectivePrice_followsProductSale() {
        LocalDateTime now = LocalDateTime.now();
        Product product = productOnSale(20, now.minusMinutes(1), now.plusMinutes(1));
        ProductSku sku = new ProductSku();
        sku.setProduct(product);
        sku.setPrice(new BigDecimal("1000.00"));

        assertThat(sku.getEffectivePrice()).isEqualByComparingTo("800.00");
    }
}
