package com.example.shopping.category.service;

import com.example.shopping.category.dto.CategoryRequest;
import com.example.shopping.category.dto.CategoryResponse;
import com.example.shopping.category.dto.CategoryStatusRequest;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.CategoryStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category existingCategory;

    @BeforeEach
    void setUp() {
        existingCategory = new Category();
        existingCategory.setId(1L);
        existingCategory.setName("男裝");
    }

    @Test
    void delete_throws_whenCategoryHasChildren() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("子分類");
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_throws_whenCategoryHasProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品");
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_succeeds_whenNoChildrenOrProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void delete_throws_whenCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_throws_whenParentIsSelf() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));

        CategoryRequest request = new CategoryRequest();
        request.setName("男裝");
        request.setParentId(1L);

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可為自己");
    }

    @Test
    void create_setsParentReference_whenParentIdProvided() {
        Category parent = new Category();
        parent.setId(5L);
        when(categoryRepository.getReferenceById(5L)).thenReturn(parent);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest request = new CategoryRequest();
        request.setName("上衣");
        request.setParentId(5L);
        request.setSortOrder(1);

        CategoryResponse response = categoryService.create(request);

        assertThat(response.getParentId()).isEqualTo(5L);
    }

    @Test
    void updateStatus_changesStatus() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        CategoryStatusRequest request = new CategoryStatusRequest();
        request.setStatus(CategoryStatus.DISABLED);

        CategoryResponse response = categoryService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(CategoryStatus.DISABLED);
    }
}
