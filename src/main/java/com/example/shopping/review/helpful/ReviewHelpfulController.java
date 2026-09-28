package com.example.shopping.review.helpful;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.AuthenticatedUser;
import com.example.shopping.security.SecurityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewHelpfulController {

    private final ReviewHelpfulService helpfulService;

    public ReviewHelpfulController(ReviewHelpfulService helpfulService) {
        this.helpfulService = helpfulService;
    }

    @PostMapping("/{reviewId}/helpful")
    public ApiResponse<ReviewHelpfulService.VoteResponse> vote(@PathVariable Long productId, @PathVariable Long reviewId) {
        return ApiResponse.success(helpfulService.vote(SecurityUtils.getCurrentUserId(), productId, reviewId));
    }

    @DeleteMapping("/{reviewId}/helpful")
    public ApiResponse<ReviewHelpfulService.VoteResponse> unvote(@PathVariable Long productId,
                                                                 @PathVariable Long reviewId) {
        return ApiResponse.success(helpfulService.unvote(SecurityUtils.getCurrentUserId(), productId, reviewId));
    }

    /**
     * 自己在這個商品按過「有幫助」的評價 id。路徑在 /api/products/** 底下,GET 對匿名開放,
     * 所以這裡未登入時回空陣列,而不是 401。
     */
    @GetMapping("/helpful/me")
    public ApiResponse<List<Long>> myVotes(@PathVariable Long productId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(helpfulService.myVotes(user.id(), productId));
    }
}
