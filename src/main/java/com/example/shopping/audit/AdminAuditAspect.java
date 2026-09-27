package com.example.shopping.audit;

import com.example.shopping.audit.entity.AdminAuditLog;
import com.example.shopping.audit.service.AuditLogService;
import com.example.shopping.security.AuthenticatedUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 攔截標了 {@link AdminAudit} 的方法,把「誰、在什麼時候、對什麼、做了什麼、成功與否」寫進操作紀錄。
 * 紀錄失敗只寫 log,不影響原本的 API 回應。
 */
@Aspect
@Component
public class AdminAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditAspect.class);

    private final AuditLogService auditLogService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    public AdminAuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, AdminAudit audit) throws Throwable {
        AuthenticatedUser admin = currentUser();
        try {
            Object result = joinPoint.proceed();
            record(joinPoint, audit, admin, result, null);
            return result;
        } catch (Throwable ex) {
            record(joinPoint, audit, admin, null, ex);
            throw ex;
        }
    }

    private void record(ProceedingJoinPoint joinPoint, AdminAudit audit, AuthenticatedUser admin,
                        Object result, Throwable error) {
        if (admin == null) {
            return;
        }
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            MethodBasedEvaluationContext context =
                    new MethodBasedEvaluationContext(null, method, joinPoint.getArgs(), parameterNames);
            context.setVariable("result", result);

            AdminAuditLog entry = new AdminAuditLog();
            entry.setAdminId(admin.id());
            entry.setAdminUsername(admin.subject());
            entry.setAction(audit.action());
            entry.setTargetType(audit.target());
            entry.setTargetId(truncate(evaluate(audit.targetId(), context), 50));
            entry.setDetail(truncate(evaluate(audit.detail(), context), 500));
            entry.setSuccess(error == null);
            entry.setErrorMessage(error == null ? null : truncate(error.getMessage(), 255));
            auditLogService.record(entry);
        } catch (RuntimeException ex) {
            log.warn("寫入管理員操作紀錄失敗:{} {}", audit.action(), ex.getMessage());
        }
    }

    private String evaluate(String expression, MethodBasedEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            Object value = expressionCache.computeIfAbsent(expression, parser::parseExpression).getValue(context);
            return value == null ? null : value.toString();
        } catch (RuntimeException ex) {
            // 例如失敗時 #result 為 null、或參數名稱對不上;不應該讓紀錄整筆失敗
            return null;
        }
    }

    private static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? user
                : null;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
