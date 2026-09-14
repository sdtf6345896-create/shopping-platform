package com.example.shopping.category.repository;

import com.example.shopping.category.entity.Category;
import com.example.shopping.common.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAscIdAsc();

    List<Category> findByStatusOrderBySortOrderAscIdAsc(CategoryStatus status);

    boolean existsByParentId(Long parentId);
}
