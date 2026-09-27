package com.example.shopping.audit.dto;

import com.example.shopping.audit.AuditTarget;
import com.example.shopping.audit.entity.AdminAuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long adminId;
    private String adminUsername;
    private String action;
    private AuditTarget targetType;
    private String targetId;
    private String detail;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AdminAuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAdminId(), log.getAdminUsername(), log.getAction(),
                log.getTargetType(), log.getTargetId(), log.getDetail(), log.isSuccess(), log.getErrorMessage(),
                log.getCreatedAt());
    }
}
