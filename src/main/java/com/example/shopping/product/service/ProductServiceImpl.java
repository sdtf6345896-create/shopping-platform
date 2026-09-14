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
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.dto.response.SkuResponse;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.example.shopping.product.repository.ProductSpecifications.*;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                               ProductSkuRepository productSkuRepository,
                               CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productSkuRepository = productSkuRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductListResponse> listPublic(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                                 String keyword, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(hasStatus(ProductStatus.ON_SALE))
                .and(hasCategoryId(categoryId))
                .and(priceGreaterOrEqual(minPrice))
                .and(priceLessOrEqual(maxPrice))
                .and(nameContains(keyword));

        return productRepository.findAll(spec, pageable).map(ProductListResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductListResponse> listAdmin(Long categoryId, ProductStatus status, String keyword,
                                                Pageable pageable) {
        Specification<Product> spec = Specification
                .where(hasStatus(status))
                .and(hasCategoryId(categoryId))
                .and(nameContains(keyword));

        return productRepository.findAll(spec, pageable).map(ProductListResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicDetail(Long id) {
        Product product = findOrThrow(id);
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new ResourceNotFoundException("商品不存在");
        }
        return ProductDetailResponse.from(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminDetail(Long id) {
        return ProductDetailResponse.from(findOrThrow(id));
    }

    @Override
    public ProductDetailResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return ProductDetailResponse.from(productRepository.save(product));
    }

    @Override
    public ProductDetailResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request);
        return ProductDetailResponse.from(product);
    }

    @Override
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    @Override
    public ProductDetailResponse updateStatus(Long id, ProductStatusRequest request) {
        Product product = findOrThrow(id);
        product.setStatus(request.getStatus());
        return ProductDetailResponse.from(product);
    }

    @Override
    public SkuResponse updateStock(Long productId, Long skuId, StockUpdateRequest request) {
        ProductSku sku = productSkuRepository.findByIdAndProductId(skuId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("規格不存在"));
        sku.setStock(request.getStock());
        return SkuResponse.from(sku);
    }

    private void applyRequest(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分類不存在"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setMainImage(request.getMainImage());
        product.replaceSkus(toSkus(request.getSkus()));
    }

    private List<ProductSku> toSkus(List<SkuRequest> skuRequests) {
        return skuRequests.stream().map(r -> {
            ProductSku sku = new ProductSku();
            sku.setSkuCode(r.getSkuCode());
            sku.setSpecName(r.getSpecName());
            sku.setPrice(r.getPrice());
            sku.setStock(r.getStock());
            return sku;
        }).toList();
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
    }
}
