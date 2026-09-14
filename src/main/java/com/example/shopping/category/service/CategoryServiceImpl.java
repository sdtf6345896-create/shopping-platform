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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getPublicTree() {
        List<Category> categories = categoryRepository.findByStatusOrderBySortOrderAscIdAsc(CategoryStatus.ACTIVE);
        return buildTree(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAscIdAsc();
        return buildTree(categories);
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        applyRequest(category, request);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        applyRequest(category, request);
        return CategoryResponse.from(category);
    }

    @Override
    public void delete(Long id) {
        findOrThrow(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException("此分類底下還有子分類,請先刪除或搬移子分類");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException("此分類底下還有商品,請先移除或搬移商品");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public CategoryResponse updateStatus(Long id, CategoryStatusRequest request) {
        Category category = findOrThrow(id);
        category.setStatus(request.getStatus());
        return CategoryResponse.from(category);
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());

        if (request.getParentId() == null) {
            category.setParent(null);
        } else {
            if (request.getParentId().equals(category.getId())) {
                throw new BusinessException("父分類不可為自己");
            }
            category.setParent(categoryRepository.getReferenceById(request.getParentId()));
        }
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分類不存在"));
    }

    private List<CategoryResponse> buildTree(List<Category> categories) {
        Map<Long, CategoryResponse> responseById = new LinkedHashMap<>();
        for (Category category : categories) {
            responseById.put(category.getId(), CategoryResponse.from(category));
        }

        List<CategoryResponse> roots = new java.util.ArrayList<>();
        for (Category category : categories) {
            CategoryResponse response = responseById.get(category.getId());
            Long parentId = response.getParentId();
            if (parentId != null && responseById.containsKey(parentId)) {
                responseById.get(parentId).getChildren().add(response);
            } else {
                roots.add(response);
            }
        }
        return roots;
    }
}
