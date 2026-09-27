package com.example.shopping.returns.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.returns.dto.request.ReturnDecisionRequest;
import com.example.shopping.returns.dto.response.ReturnRequestResponse;
import com.example.shopping.returns.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/returns")
public class AdminReturnController {

    private final ReturnService returnService;

    public AdminReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReturnRequestResponse>> list(
            @RequestParam(required = false) ReturnStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(returnService.listAdmin(status, pageable)));
    }

    @AdminAudit(action = "核准退貨", target = AuditTarget.RETURN, detail = "#request?.note")
    @PostMapping("/{id}/approve")
    public ApiResponse<ReturnRequestResponse> approve(@PathVariable Long id,
                                                      @Valid @RequestBody(required = false) ReturnDecisionRequest request) {
        return ApiResponse.success("已核准退貨並退款",
                returnService.approve(id, request == null ? new ReturnDecisionRequest() : request));
    }

    @AdminAudit(action = "拒絕退貨", target = AuditTarget.RETURN, detail = "#request?.note")
    @PostMapping("/{id}/reject")
    public ApiResponse<ReturnRequestResponse> reject(@PathVariable Long id,
                                                     @Valid @RequestBody(required = false) ReturnDecisionRequest request) {
        return ApiResponse.success("已拒絕退貨申請",
                returnService.reject(id, request == null ? new ReturnDecisionRequest() : request));
    }
}
