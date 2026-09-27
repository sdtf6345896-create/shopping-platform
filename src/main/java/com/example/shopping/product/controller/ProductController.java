package com.example.shopping.product.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.product.dto.response.ProductDetailResponse;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.service.ProductService;
import com.example.shopping.search.service.SearchKeywordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final SearchKeywordService searchKeywordService;

    public ProductController(ProductService productService, SearchKeywordService searchKeywordService) {
        this.productService = productService;
        this.searchKeywordService = searchKeywordService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductListResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean inStock,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<ProductListResponse> page = productService.listPublic(categoryId, minPrice, maxPrice, keyword, inStock,
                pageable);
        // 只在第一頁、且有搜尋結果時計入熱門搜尋(翻頁不重複計算,打錯字的關鍵字不會上榜)
        if (keyword != null && pageable.getPageNumber() == 0 && page.getTotalElements() > 0) {
            searchKeywordService.record(keyword);
        }
        return ApiResponse.success(PageResponse.from(page));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<ProductListResponse>> suggestions(@RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(productService.suggest(keyword, limit));
    }

    @GetMapping("/hot-searches")
    public ApiResponse<List<String>> hotSearches(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(searchKeywordService.hot(limit));
    }

    @GetMapping("/flash-sale")
    public ApiResponse<List<ProductListResponse>> flashSale(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(productService.listFlashSale(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(productService.getPublicDetail(id));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<ProductListResponse>> related(@PathVariable Long id,
                                                           @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(productService.listRelated(id, limit));
    }
}
