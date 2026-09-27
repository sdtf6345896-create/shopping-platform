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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.shopping.product.repository.ProductSpecifications.*;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductServiceImpl(ProductRepository productRepository,
                               ProductSkuRepository productSkuRepository,
                               CategoryRepository categoryRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.productSkuRepository = productSkuRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductListResponse> listPublic(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                                 String keyword, boolean inStockOnly, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(hasStatus(ProductStatus.ON_SALE))
                .and(hasCategoryId(categoryId))
                .and(priceGreaterOrEqual(minPrice))
                .and(priceLessOrEqual(maxPrice))
                .and(nameContains(keyword))
                .and(inStock(inStockOnly));

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
    public List<ProductListResponse> listRelated(Long productId, int limit) {
        Product product = findOrThrow(productId);
        int size = Math.min(Math.max(limit, 1), 20);

        List<Product> related = new ArrayList<>(productRepository
                .findByCategoryIdAndStatusAndIdNotOrderBySalesCountDescIdDesc(
                        product.getCategory().getId(), ProductStatus.ON_SALE, productId, PageRequest.of(0, size)));

        if (related.size() < size) {
            Set<Long> exclude = new HashSet<>();
            exclude.add(productId);
            related.forEach(p -> exclude.add(p.getId()));
            related.addAll(productRepository.findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
                    ProductStatus.ON_SALE, exclude, PageRequest.of(0, size - related.size())));
        }
        return related.stream().map(ProductListResponse::from).toList();
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
        product.replaceSkus(toSkus(request.getSkus()));
        return ProductDetailResponse.from(productRepository.save(product));
    }

    @Override
    public ProductDetailResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request);

        // 以 SKU 編號合併,既有規格保留 id;真的被移除的規格先讓其他模組確認能不能刪(有訂單就擋下)
        List<ProductSku> removed = product.mergeSkus(toSkus(request.getSkus()));
        if (!removed.isEmpty()) {
            eventPublisher.publishEvent(new SkusRemovingEvent(removed.stream()
                    .map(sku -> new SkusRemovingEvent.RemovedSku(sku.getId(),
                            product.getName() + " " + sku.getSpecName()))
                    .toList()));
        }
        return ProductDetailResponse.from(product);
    }

    @Override
    public void delete(Long id) {
        Product product = findOrThrow(id);
        // 讓訂單、購物車、收藏等模組先處理自己的關聯資料;已有訂單的商品會在這裡被擋下
        eventPublisher.publishEvent(new ProductDeletingEvent(product.getId(), product.getName()));
        productRepository.delete(product);
    }

    @Override
    public ProductDetailResponse updateStatus(Long id, ProductStatusRequest request) {
        Product product = findOrThrow(id);
        product.setStatus(request.getStatus());
        return ProductDetailResponse.from(product);
    }

    @Override
    public int updateStatusBatch(BatchProductStatusRequest request) {
        return productRepository.updateStatusByIds(Set.copyOf(request.getIds()), request.getStatus());
    }

    @Override
    public SkuResponse updateStock(Long productId, Long skuId, StockUpdateRequest request) {
        ProductSku sku = productSkuRepository.findByIdAndProductId(skuId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("規格不存在"));
        sku.setStock(request.getStock());
        return SkuResponse.from(sku);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockSkuResponse> listLowStock(int threshold, int limit) {
        if (threshold < 0) {
            throw new BusinessException("庫存門檻不可為負數");
        }
        int size = Math.min(Math.max(limit, 1), 200);
        return productSkuRepository.findLowStock(ProductStatus.ON_SALE, threshold, PageRequest.of(0, size))
                .stream()
                .map(LowStockSkuResponse::from)
                .toList();
    }

    private static void applySale(Product product, ProductRequest request) {
        Integer percent = request.getSaleDiscountPercent();
        if (percent == null) {
            product.setSaleDiscountPercent(null);
            product.setSaleStartAt(null);
            product.setSaleEndAt(null);
            return;
        }
        if (request.getSaleStartAt() == null || request.getSaleEndAt() == null) {
            throw new BusinessException("設定限時特價需填寫開始與結束時間");
        }
        if (!request.getSaleEndAt().isAfter(request.getSaleStartAt())) {
            throw new BusinessException("特價結束時間必須晚於開始時間");
        }
        product.setSaleDiscountPercent(percent);
        product.setSaleStartAt(request.getSaleStartAt());
        product.setSaleEndAt(request.getSaleEndAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductListResponse> suggest(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 50) {
            trimmed = trimmed.substring(0, 50);
        }
        int size = Math.min(Math.max(limit, 1), 10);
        return productRepository.findByStatusAndNameContainingIgnoreCaseOrderBySalesCountDescIdDesc(
                        ProductStatus.ON_SALE, trimmed, PageRequest.of(0, size))
                .stream()
                .map(ProductListResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductListResponse> listFlashSale(int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        return productRepository.findOnSale(ProductStatus.ON_SALE, LocalDateTime.now(), PageRequest.of(0, size))
                .stream()
                .map(ProductListResponse::from)
                .toList();
    }

    private void applyRequest(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("分類不存在"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setMainImage(request.getMainImage());
        product.replaceImages(request.getImages());
        applySale(product, request);
    }

    private List<ProductSku> toSkus(List<SkuRequest> skuRequests) {
        Set<String> codes = new HashSet<>();
        for (SkuRequest r : skuRequests) {
            if (!codes.add(r.getSkuCode())) {
                throw new BusinessException("SKU 編號重複:" + r.getSkuCode());
            }
        }
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
