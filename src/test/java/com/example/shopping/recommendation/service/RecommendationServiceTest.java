package com.example.shopping.recommendation.service;

import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.recommendation.dto.RecommendationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private BrowsingHistoryItemRepository historyRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private static Category category(long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private static Product product(long id, Category category) {
        Product product = new Product();
        product.setId(id);
        product.setName("商品" + id);
        product.setCategory(category);
        product.setStatus(ProductStatus.ON_SALE);
        return product;
    }

    private static BrowsingHistoryItem viewed(Product product) {
        BrowsingHistoryItem item = new BrowsingHistoryItem();
        item.setProduct(product);
        return item;
    }

    private void stubHistory(List<BrowsingHistoryItem> items) {
        when(historyRepository.findAllByMemberIdWithDetails(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(items));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recommend_ranksCategoriesByViews_andExcludesViewedProducts() {
        Category shirts = category(10L);
        Category bottles = category(20L);
        stubHistory(List.of(
                viewed(product(1L, bottles)),
                viewed(product(2L, shirts)),
                viewed(product(3L, shirts))));
        when(productRepository.findByCategoryIdInAndStatusAndIdNotInOrderBySalesCountDescIdDesc(
                any(), eq(ProductStatus.ON_SALE), any(), eq(PageRequest.of(0, 2))))
                .thenReturn(List.of(product(4L, shirts), product(5L, bottles)));

        RecommendationResponse response = recommendationService.recommend(1L, 2);

        assertThat(response.isPersonalized()).isTrue();
        assertThat(response.getProducts()).extracting(ProductListResponse::getId).containsExactly(4L, 5L);

        ArgumentCaptor<Collection<Long>> categories = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<Long>> excluded = ArgumentCaptor.forClass(Collection.class);
        verify(productRepository).findByCategoryIdInAndStatusAndIdNotInOrderBySalesCountDescIdDesc(
                categories.capture(), eq(ProductStatus.ON_SALE), excluded.capture(), any());
        assertThat(categories.getValue()).containsExactly(10L, 20L);
        assertThat(excluded.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
        verify(productRepository, never()).findByStatusAndIdNotInOrderBySalesCountDescIdDesc(any(), any(), any());
    }

    @Test
    void recommend_fillsWithBestSellers_whenCategoriesRunOut() {
        Category shirts = category(10L);
        stubHistory(List.of(viewed(product(2L, shirts))));
        when(productRepository.findByCategoryIdInAndStatusAndIdNotInOrderBySalesCountDescIdDesc(
                any(), any(), any(), any())).thenReturn(List.of(product(4L, shirts)));
        when(productRepository.findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
                ProductStatus.ON_SALE, Set.of(2L, 4L), PageRequest.of(0, 2)))
                .thenReturn(List.of(product(8L, category(30L)), product(9L, category(30L))));

        RecommendationResponse response = recommendationService.recommend(1L, 3);

        assertThat(response.getProducts()).extracting(ProductListResponse::getId).containsExactly(4L, 8L, 9L);
    }

    @Test
    void recommend_withoutHistory_returnsBestSellers_notPersonalized() {
        stubHistory(List.of());
        when(productRepository.findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
                ProductStatus.ON_SALE, Set.of(-1L), PageRequest.of(0, 8)))
                .thenReturn(List.of(product(8L, category(30L))));

        RecommendationResponse response = recommendationService.recommend(1L, 8);

        assertThat(response.isPersonalized()).isFalse();
        assertThat(response.getProducts()).hasSize(1);
        verify(productRepository, never()).findByCategoryIdInAndStatusAndIdNotInOrderBySalesCountDescIdDesc(
                any(), any(), any(), any());
    }
}
