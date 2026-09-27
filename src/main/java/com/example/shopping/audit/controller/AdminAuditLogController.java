package com.example.shopping.audit.controller;

import com.example.shopping.audit.AuditTarget;
import com.example.shopping.audit.dto.AuditLogResponse;
import com.example.shopping.audit.service.AuditLogService;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) String adminUsername,
            @RequestParam(required = false) AuditTarget targetType,
            @RequestParam(required = false) String targetId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                auditLogService.search(adminUsername, targetType, targetId, pageable)));
    }
}
