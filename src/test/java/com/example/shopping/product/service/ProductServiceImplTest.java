package com.example.shopping.product.service;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.product.dto.request.BatchProductStatusRequest;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.SkuRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.LowStockSkuResponse;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(2L);

        existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setCategory(category);
        existingProduct.setName("經典白T恤");
        existingProduct.setPrice(BigDecimal.valueOf(299));
        existingProduct.setStatus(ProductStatus.ON_SALE);
    }

    @Test
    void getPublicDetail_throws_whenProductOffShelf() {
        existingProduct.setStatus(ProductStatus.OFF_SHELF);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        assertThatThrownBy(() -> productService.getPublicDetail(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicDetail_returns_whenProductOnSale() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        ProductDetailResponse response = productService.getPublicDetail(1L);

        assertThat(response.getName()).isEqualTo("經典白T恤");
    }

    @Test
    void getPublicDetail_throws_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getPublicDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throws_whenCategoryNotFound() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("新商品");
        request.setPrice(BigDecimal.TEN);
        request.setSkus(List.of(skuRequest()));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分類不存在");
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_savesProduct_withCategoryAndSkus() {
        Category category = new Category();
        category.setId(5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("新商品");
        request.setPrice(BigDecimal.TEN);
        request.setSkus(List.of(skuRequest()));

        ProductDetailResponse response = productService.create(request);

        assertThat(response.getName()).isEqualTo("新商品");
        assertThat(response.getSkus()).hasSize(1);
    }

    @Test
    void update_replacesExistingSkus() {
        ProductSku oldSku = new ProductSku();
        oldSku.setId(10L);
        oldSku.setSkuCode("OLD-001");
        existingProduct.replaceSkus(List.of(oldSku));

        Category category = new Category();
        category.setId(5L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("更新後名稱");
        request.setPrice(BigDecimal.valueOf(399));
        request.setSkus(List.of(skuRequest()));

        ProductDetailResponse response = productService.update(1L, request);

        assertThat(response.getName()).isEqualTo("更新後名稱");
        assertThat(existingProduct.getSkus()).hasSize(1);
        assertThat(existingProduct.getSkus().get(0).getSkuCode()).isEqualTo("SKU-001");
    }

    @Test
    void update_replacesGalleryImagesInGivenOrder() {
        existingProduct.replaceImages(List.of("https://img/old.jpg"));
        Category category = new Category();
        category.setId(5L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("經典圓領T恤");
        request.setPrice(BigDecimal.valueOf(590));
        request.setSkus(List.of(skuRequest()));
        request.setImages(List.of("https://img/b.jpg", " https://img/a.jpg "));

        ProductDetailResponse response = productService.update(1L, request);

        assertThat(response.getImages()).containsExactly("https://img/b.jpg", "https://img/a.jpg");
        assertThat(existingProduct.getImages()).extracting("sortOrder").containsExactly(0, 1);
    }

    @Test
    void update_clearsGallery_whenImagesOmitted() {
        existingProduct.replaceImages(List.of("https://img/old.jpg"));
        Category category = new Category();
        category.setId(5L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("經典圓領T恤");
        request.setPrice(BigDecimal.valueOf(590));
        request.setSkus(List.of(skuRequest()));

        assertThat(productService.update(1L, request).getImages()).isEmpty();
    }

    @Test
    void delete_removesProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        productService.delete(1L);

        verify(productRepository).delete(existingProduct);
        verify(eventPublisher).publishEvent(any(ProductDeletingEvent.class));
    }

    @Test
    void delete_throws_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_changesStatus() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        ProductStatusRequest request = new ProductStatusRequest();
        request.setStatus(ProductStatus.OFF_SHELF);

        ProductDetailResponse response = productService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.OFF_SHELF);
    }

    @Test
    void updateStock_updatesSkuStock() {
        ProductSku sku = new ProductSku();
        sku.setId(10L);
        sku.setSkuCode("SKU-001");
        sku.setStock(3);
        when(productSkuRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(sku));

        StockUpdateRequest request = new StockUpdateRequest();
        request.setStock(50);

        SkuResponse response = productService.updateStock(1L, 10L, request);

        assertThat(response.getStock()).isEqualTo(50);
        assertThat(sku.getStock()).isEqualTo(50);
    }

    @Test
    void updateStock_throws_whenSkuNotBelongToProduct() {
        when(productSkuRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.empty());

        StockUpdateRequest request = new StockUpdateRequest();
        request.setStock(50);

        assertThatThrownBy(() -> productService.updateStock(1L, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private SkuRequest skuRequest() {
        SkuRequest sku = new SkuRequest();
        sku.setSkuCode("SKU-001");
        sku.setSpecName("均一尺寸");
        sku.setPrice(BigDecimal.TEN);
        sku.setStock(10);
        return sku;
    }

    @Test
    void listLowStock_mapsSkusFromRepository() {
        Product product = new Product();
        product.setId(10L);
        product.setName("經典圓領T恤");
        ProductSku sku = new ProductSku();
        sku.setId(3L);
        sku.setProduct(product);
        sku.setSkuCode("TSHIRT-BLK-M");
        sku.setSpecName("黑色/M");
        sku.setStock(2);
        when(productSkuRepository.findLowStock(ProductStatus.ON_SALE, 5, PageRequest.of(0, 20)))
                .thenReturn(List.of(sku));

        List<LowStockSkuResponse> result = productService.listLowStock(5, 20);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.getProductId()).isEqualTo(10L);
            assertThat(r.getSkuCode()).isEqualTo("TSHIRT-BLK-M");
            assertThat(r.getStock()).isEqualTo(2);
        });
    }

    @Test
    void listLowStock_capsLimit() {
        when(productSkuRepository.findLowStock(ProductStatus.ON_SALE, 10, PageRequest.of(0, 200)))
                .thenReturn(List.of());

        assertThat(productService.listLowStock(10, 10_000)).isEmpty();
    }

    @Test
    void listLowStock_rejectsNegativeThreshold() {
        assertThatThrownBy(() -> productService.listLowStock(-1, 20))
                .isInstanceOf(BusinessException.class);
    }

    private Product onSaleProduct(long id, Category category) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品" + id);
        p.setCategory(category);
        p.setStatus(ProductStatus.ON_SALE);
        return p;
    }

    @Test
    void listRelated_prefersSameCategory_thenFillsWithBestSellers() {
        Category category = new Category();
        category.setId(7L);
        Product current = onSaleProduct(1L, category);
        Product sibling = onSaleProduct(2L, category);
        Product other = onSaleProduct(3L, new Category());

        when(productRepository.findById(1L)).thenReturn(Optional.of(current));
        when(productRepository.findByCategoryIdAndStatusAndIdNotOrderBySalesCountDescIdDesc(
                7L, ProductStatus.ON_SALE, 1L, PageRequest.of(0, 2))).thenReturn(List.of(sibling));
        when(productRepository.findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
                ProductStatus.ON_SALE, Set.of(1L, 2L), PageRequest.of(0, 1))).thenReturn(List.of(other));

        List<ProductListResponse> result = productService.listRelated(1L, 2);

        assertThat(result).extracting(ProductListResponse::getId).containsExactly(2L, 3L);
    }

    @Test
    void listRelated_skipsFallback_whenSameCategoryIsEnough() {
        Category category = new Category();
        category.setId(7L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(onSaleProduct(1L, category)));
        when(productRepository.findByCategoryIdAndStatusAndIdNotOrderBySalesCountDescIdDesc(
                7L, ProductStatus.ON_SALE, 1L, PageRequest.of(0, 1)))
                .thenReturn(List.of(onSaleProduct(2L, category)));

        assertThat(productService.listRelated(1L, 1)).hasSize(1);
        verify(productRepository, never()).findByStatusAndIdNotInOrderBySalesCountDescIdDesc(any(), any(), any());
    }

    private ProductRequest saleRequest(Integer percent, LocalDateTime start, LocalDateTime end) {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("經典圓領T恤");
        request.setPrice(BigDecimal.valueOf(590));
        request.setSkus(List.of(skuRequest()));
        request.setSaleDiscountPercent(percent);
        request.setSaleStartAt(start);
        request.setSaleEndAt(end);
        return request;
    }

    private void stubUpdate() {
        Category category = new Category();
        category.setId(5L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
    }

    @Test
    void update_setsFlashSale_andReportsSalePriceWhileActive() {
        stubUpdate();
        LocalDateTime now = LocalDateTime.now();

        ProductDetailResponse response = productService.update(1L,
                saleRequest(20, now.minusHours(1), now.plusHours(1)));

        assertThat(response.getSaleDiscountPercent()).isEqualTo(20);
        assertThat(response.getSalePrice()).isEqualByComparingTo("472.00");
        assertThat(response.getSkus().get(0).getSalePrice()).isNotNull();
    }

    @Test
    void update_rejectsSaleWithoutPeriodOrWithReversedPeriod() {
        stubUpdate();
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> productService.update(1L, saleRequest(20, null, now)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("開始與結束時間");
        assertThatThrownBy(() -> productService.update(1L, saleRequest(20, now, now.minusMinutes(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("晚於開始時間");
    }

    @Test
    void update_clearsSale_whenPercentOmitted() {
        existingProduct.setSaleDiscountPercent(30);
        existingProduct.setSaleStartAt(LocalDateTime.now().minusDays(1));
        existingProduct.setSaleEndAt(LocalDateTime.now().plusDays(1));
        stubUpdate();

        ProductDetailResponse response = productService.update(1L, saleRequest(null, null, null));

        assertThat(response.getSalePrice()).isNull();
        assertThat(existingProduct.getSaleEndAt()).isNull();
    }

    @Test
    void update_keepsExistingSkuIds_andOnlyReportsRemovedSkus() {
        ProductSku kept = new ProductSku();
        kept.setId(10L);
        kept.setSkuCode("SKU-001");
        kept.setSpecName("舊名稱");
        ProductSku dropped = new ProductSku();
        dropped.setId(11L);
        dropped.setSkuCode("OLD-002");
        dropped.setSpecName("停產款");
        existingProduct.replaceSkus(List.of(kept, dropped));
        Category category = new Category();
        category.setId(5L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("經典圓領T恤");
        request.setPrice(BigDecimal.valueOf(590));
        request.setSkus(List.of(skuRequest()));

        productService.update(1L, request);

        // 同編號的規格就地更新,id 不變(訂單明細仍指向它)
        assertThat(existingProduct.getSkus()).singleElement().satisfies(sku -> {
            assertThat(sku.getId()).isEqualTo(10L);
            assertThat(sku.getStock()).isEqualTo(10);
        });
        ArgumentCaptor<SkusRemovingEvent> event = ArgumentCaptor.forClass(SkusRemovingEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().skuIds()).containsExactly(11L);
    }

    @Test
    void update_rejectsDuplicateSkuCodes() {
        Category category = new Category();
        category.setId(5L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest();
        request.setCategoryId(5L);
        request.setName("經典圓領T恤");
        request.setPrice(BigDecimal.valueOf(590));
        request.setSkus(List.of(skuRequest(), skuRequest()));

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU 編號重複");
    }

    @Test
    void suggest_trimsKeyword_andCapsLimit() {
        Product shirt = new Product();
        shirt.setId(3L);
        shirt.setName("經典圓領T恤");
        when(productRepository.findByStatusAndNameContainingIgnoreCaseOrderBySalesCountDescIdDesc(
                ProductStatus.ON_SALE, "T恤", PageRequest.of(0, 10))).thenReturn(List.of(shirt));

        List<ProductListResponse> result = productService.suggest("  T恤 ", 99);

        assertThat(result).extracting(ProductListResponse::getName).containsExactly("經典圓領T恤");
    }

    @Test
    void suggest_returnsNothing_forBlankKeyword() {
        assertThat(productService.suggest("   ", 8)).isEmpty();
        verify(productRepository, never()).findByStatusAndNameContainingIgnoreCaseOrderBySalesCountDescIdDesc(
                any(), any(), any());
    }

    @Test
    void updateStatusBatch_deduplicatesIds() {
        BatchProductStatusRequest request = new BatchProductStatusRequest();
        request.setIds(List.of(1L, 2L, 2L));
        request.setStatus(ProductStatus.OFF_SHELF);
        when(productRepository.updateStatusByIds(Set.of(1L, 2L), ProductStatus.OFF_SHELF)).thenReturn(2);

        assertThat(productService.updateStatusBatch(request)).isEqualTo(2);
    }
}
