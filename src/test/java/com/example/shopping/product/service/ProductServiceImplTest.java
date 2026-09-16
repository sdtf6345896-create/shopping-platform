package com.example.shopping.product.service;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.product.dto.request.ProductRequest;
import com.example.shopping.product.dto.request.ProductStatusRequest;
import com.example.shopping.product.dto.request.SkuRequest;
import com.example.shopping.product.dto.request.StockUpdateRequest;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void delete_removesProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        productService.delete(1L);

        verify(productRepository).delete(existingProduct);
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
}
