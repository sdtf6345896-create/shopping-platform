package com.example.shopping.recommendation.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.recommendation.dto.BoughtTogetherResponse;
import com.example.shopping.recommendation.repository.CoPurchaseProjection;
import com.example.shopping.recommendation.repository.CoPurchaseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 「買了這個的人也買了」:依真實訂單的共同購買次數推薦 */
@Service
@Transactional(readOnly = true)
public class BoughtTogetherService {

    /** 只統計成立且付過款的訂單(不含待付款、取消、退款) */
    static final EnumSet<OrderStatus> COUNTED_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    private final CoPurchaseRepository coPurchaseRepository;
    private final ProductRepository productRepository;

    public BoughtTogetherService(CoPurchaseRepository coPurchaseRepository, ProductRepository productRepository) {
        this.coPurchaseRepository = coPurchaseRepository;
        this.productRepository = productRepository;
    }

    public List<BoughtTogetherResponse> boughtTogether(Long productId, int limit) {
        int size = Math.min(Math.max(limit, 1), 12);
        List<CoPurchaseProjection> rows = coPurchaseRepository.findBoughtTogether(
                productId, ProductStatus.ON_SALE, COUNTED_STATUSES, PageRequest.of(0, size));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = productRepository.findAllById(
                        rows.stream().map(CoPurchaseProjection::getProductId).toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return rows.stream()
                .filter(row -> products.containsKey(row.getProductId()))
                .map(row -> new BoughtTogetherResponse(ProductListResponse.from(products.get(row.getProductId())),
                        row.getOrderCount()))
                .toList();
    }
}
