package com.example.shopping.audit.service;

import com.example.shopping.audit.AuditTarget;
import com.example.shopping.audit.dto.AuditLogResponse;
import com.example.shopping.audit.entity.AdminAuditLog;
import com.example.shopping.audit.repository.AdminAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AdminAuditLogRepository repository;

    public AuditLogService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    /** 獨立交易寫入,就算外層操作回滾,失敗紀錄也會留下 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AdminAuditLog log) {
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String adminUsername, AuditTarget targetType, String targetId,
                                         Pageable pageable) {
        Specification<AdminAuditLog> spec = Specification
                .where(equalsIfPresent("adminUsername", blankToNull(adminUsername)))
                .and(equalsIfPresent("targetType", targetType))
                .and(equalsIfPresent("targetId", blankToNull(targetId)));
        return repository.findAll(spec, pageable).map(AuditLogResponse::from);
    }

    private static Specification<AdminAuditLog> equalsIfPresent(String field, Object value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(field), value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
