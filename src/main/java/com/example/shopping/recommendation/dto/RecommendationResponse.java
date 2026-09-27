package com.example.shopping.recommendation.dto;

import com.example.shopping.product.dto.response.ProductListResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecommendationResponse {

    /** true 表示依會員瀏覽紀錄推薦;false 表示沒有紀錄,只回傳熱銷商品 */
    private boolean personalized;
    private List<ProductListResponse> products;
}
