package com.example.shopping.promotion;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    public AdminPromotionController(AdminPromotionService adminPromotionService) {
        this.adminPromotionService = adminPromotionService;
    }

    @GetMapping
    public ApiResponse<List<PromotionResponse>> list() {
        return ApiResponse.success(adminPromotionService.list());
    }

    @AdminAudit(action = "新增滿件活動", target = AuditTarget.PROMOTION, detail = "#request.name")
    @PostMapping
    public ApiResponse<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success("新增成功", adminPromotionService.create(request));
    }

    @AdminAudit(action = "修改滿件活動", target = AuditTarget.PROMOTION, detail = "#request.name")
    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success("更新成功", adminPromotionService.update(id, request));
    }

    @AdminAudit(action = "滿件活動啟用/停用", target = AuditTarget.PROMOTION, detail = "#request.active")
    @PatchMapping("/{id}/active")
    public ApiResponse<PromotionResponse> setActive(@PathVariable Long id,
                                                    @Valid @RequestBody PromotionActiveRequest request) {
        return ApiResponse.success("狀態更新成功", adminPromotionService.setActive(id, request.getActive()));
    }

    @AdminAudit(action = "刪除滿件活動", target = AuditTarget.PROMOTION)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminPromotionService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
