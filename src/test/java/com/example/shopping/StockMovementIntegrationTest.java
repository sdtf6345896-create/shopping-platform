package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.SkuRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.service.ProductService;
import com.example.shopping.product.service.StockImportService;
import com.example.shopping.product.stock.StockMovement;
import com.example.shopping.product.stock.StockMovementRepository;
import com.example.shopping.product.stock.StockReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 新增商品、編輯規格、手動調整、CSV 匯入都會留下正確的異動量與異動後庫存;刪除規格時紀錄一併清掉 */
@SpringBootTest
@ActiveProfiles("test")
class StockMovementIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private StockImportService stockImportService;
    @Autowired
    private StockMovementRepository movementRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    private SkuRequest sku(String code, int stock) {
        SkuRequest sku = new SkuRequest();
        sku.setSkuCode(code + "-" + suffix);
        sku.setSpecName(code);
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(stock);
        return sku;
    }

    private ProductRequest request(Long categoryId, List<SkuRequest> skus) {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(categoryId);
        request.setName("庫存紀錄測試");
        request.setPrice(new BigDecimal("100"));
        request.setSkus(skus);
        return request;
    }

    private List<StockMovement> movementsOf(Long skuId) {
        List<StockMovement> newestFirst = new ArrayList<>(
                movementRepository.findBySkuIdOrderByIdDesc(skuId, PageRequest.of(0, 20)).getContent());
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    @Test
    void recordsEveryKindOfChange() {
        Category category = new Category();
        category.setName("庫存紀錄-" + suffix);
        categoryRepository.save(category);

        ProductDetailResponse created = productService.create(request(category.getId(),
                List.of(sku("RED", 10), sku("BLUE", 0))));
        Long redId = created.getSkus().stream().filter(s -> s.getSpecName().equals("RED")).findFirst().orElseThrow().getId();
        Long blueId = created.getSkus().stream().filter(s -> s.getSpecName().equals("BLUE")).findFirst().orElseThrow().getId();

        // 編輯商品:RED 改 12、移除 BLUE、新增 GREEN 5
        ProductDetailResponse updated = productService.update(created.getId(), request(category.getId(),
                List.of(sku("RED", 12), sku("GREEN", 5))));
        Long greenId = updated.getSkus().stream().filter(s -> s.getSpecName().equals("GREEN")).findFirst().orElseThrow().getId();

        StockUpdateRequest manual = new StockUpdateRequest();
        manual.setStock(7);
        productService.updateStock(created.getId(), redId, manual);

        stockImportService.importCsv(new MockMultipartFile("file", "restock.csv", "text/csv",
                ("sku,stock\nRED-" + suffix + ",20\n").getBytes(StandardCharsets.UTF_8)));

        assertThat(movementsOf(redId))
                .extracting(StockMovement::getReason, StockMovement::getChangeQty, StockMovement::getStockAfter,
                        StockMovement::getReference)
                .containsExactly(
                        tuple(StockReason.INITIAL, 10, 10, null),
                        tuple(StockReason.PRODUCT_EDIT, 2, 12, null),
                        tuple(StockReason.MANUAL, -5, 7, null),
                        tuple(StockReason.IMPORT, 13, 20, "restock.csv"));
        assertThat(movementsOf(greenId)).extracting(StockMovement::getReason, StockMovement::getChangeQty)
                .containsExactly(tuple(StockReason.INITIAL, 5));
        // 初始庫存 0 的規格沒有紀錄;被移除的規格也不會殘留紀錄
        assertThat(movementsOf(blueId)).isEmpty();
    }
}
