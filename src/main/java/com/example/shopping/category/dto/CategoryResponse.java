package com.example.shopping.category.dto;

import com.example.shopping.category.entity.Category;
import com.example.shopping.common.enums.CategoryStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryResponse {

    private Long id;
    private String name;
    private Long parentId;
    private int sortOrder;
    private CategoryStatus status;
    private List<CategoryResponse> children = new ArrayList<>();

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        response.setSortOrder(category.getSortOrder());
        response.setStatus(category.getStatus());
        return response;
    }
}
