package com.example.shopping.member.cvs;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.order.shipping.CvsPickupRequest;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/cvs-stores")
public class MemberCvsStoreController {

    private final MemberCvsStoreService storeService;

    public MemberCvsStoreController(MemberCvsStoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ApiResponse<List<MemberCvsStoreService.StoreResponse>> list() {
        return ApiResponse.success(storeService.list(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public ApiResponse<MemberCvsStoreService.StoreResponse> save(@Valid @RequestBody CvsPickupRequest request) {
        return ApiResponse.success("已儲存常用門市", storeService.save(SecurityUtils.getCurrentUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        storeService.delete(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.success("已刪除", null);
    }
}
