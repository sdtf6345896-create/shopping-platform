package com.example.shopping.product.service;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.product.dto.response.StockImportResponse;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockImportServiceTest {

    @Mock
    private ProductSkuRepository productSkuRepository;

    @InjectMocks
    private StockImportService stockImportService;

    private ProductSku shirt;
    private ProductSku bottle;

    @BeforeEach
    void setUp() {
        shirt = sku("TSHIRT-BLK-M", 3);
        bottle = sku("BOTTLE-WHT", 0);
    }

    private static ProductSku sku(String code, int stock) {
        ProductSku sku = new ProductSku();
        sku.setSkuCode(code);
        sku.setStock(stock);
        return sku;
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "stock.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importCsv_appliesAllRows_ignoringBomHeaderQuotesAndBlankLines() {
        when(productSkuRepository.findBySkuCodeIn(any())).thenReturn(List.of(shirt, bottle));

        StockImportResponse result = stockImportService.importCsv(
                csv("﻿sku_code,stock\r\nTSHIRT-BLK-M,10\r\n\r\n\"BOTTLE-WHT\", \"25\"\r\n"));

        assertThat(result.applied()).isTrue();
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.updated()).isEqualTo(2);
        assertThat(shirt.getStock()).isEqualTo(10);
        assertThat(bottle.getStock()).isEqualTo(25);
    }

    @Test
    void importCsv_updatesNothing_whenAnyRowIsInvalid_andReportsEachLine() {
        when(productSkuRepository.findBySkuCodeIn(any())).thenReturn(List.of(shirt, bottle));

        StockImportResponse result = stockImportService.importCsv(csv("""
                sku,stock
                TSHIRT-BLK-M,10
                BOTTLE-WHT,-1
                UNKNOWN-SKU,5
                BOTTLE-WHT,abc
                TSHIRT-BLK-M,7
                ONLY-ONE-COLUMN
                """));

        assertThat(result.applied()).isFalse();
        assertThat(result.updated()).isZero();
        assertThat(result.totalRows()).isEqualTo(6);
        assertThat(result.errors()).extracting(StockImportResponse.RowError::line).containsExactly(3, 4, 5, 6, 7);
        assertThat(result.errors().get(0).message()).contains("0 到");
        assertThat(result.errors().get(1).message()).contains("找不到 SKU");
        assertThat(result.errors().get(2).message()).contains("整數");
        assertThat(result.errors().get(3).message()).contains("重複");
        // 全有或全無:正確的那一列也沒有被套用
        assertThat(shirt.getStock()).isEqualTo(3);
    }

    @Test
    void importCsv_rejectsEmptyFile() {
        assertThatThrownBy(() -> stockImportService.importCsv(csv("sku,stock\n\n")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("沒有資料");
    }
}
