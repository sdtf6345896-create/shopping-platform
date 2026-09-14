package com.example.shopping.category.service;

import com.example.shopping.category.dto.CategoryRequest;
import com.example.shopping.category.dto.CategoryResponse;
import com.example.shopping.category.dto.CategoryStatusRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getPublicTree();

    List<CategoryResponse> listAll();

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    CategoryResponse updateStatus(Long id, CategoryStatusRequest request);
}
