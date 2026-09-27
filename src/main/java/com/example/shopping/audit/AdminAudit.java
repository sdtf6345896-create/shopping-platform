package com.example.shopping.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標在後台 controller 方法上,呼叫後(不論成功或失敗)自動寫一筆 admin_audit_log。
 * <p>
 * {@link #targetId()} 與 {@link #detail()} 是 SpEL,可以用方法參數名稱(例如 {@code #id}、{@code #request.status}),
 * 成功時另外可用 {@code #result}(controller 回傳的 ApiResponse)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminAudit {

    /** 操作名稱,例如「更新訂單狀態」 */
    String action();

    AuditTarget target();

    /** 預設取路徑上的 id;新增類操作則從回傳結果取 */
    String targetId() default "#id ?: #result?.data?.id";

    /** 額外記錄的內容,空字串表示不記錄 */
    String detail() default "";
}
