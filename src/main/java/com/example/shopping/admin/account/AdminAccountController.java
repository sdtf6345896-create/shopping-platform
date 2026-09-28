package com.example.shopping.admin.account;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AdminAccountController {

    private final AdminAccountService accountService;

    public AdminAccountController(AdminAccountService accountService) {
        this.accountService = accountService;
    }

    public record CreateRequest(
            @NotBlank(message = "帳號不可為空")
            @Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$", message = "帳號需為 3~50 碼英數字(可含 _ . -)")
            String username,
            @NotBlank(message = "姓名不可為空") @Size(max = 50, message = "姓名最多 50 字") String name,
            @NotBlank(message = "密碼不可為空") @Size(min = 8, max = 100, message = "密碼長度需為 8~100 碼") String password,
            @NotNull(message = "請選擇角色") Role role) {
    }

    public record UpdateRequest(
            @NotBlank(message = "姓名不可為空") @Size(max = 50, message = "姓名最多 50 字") String name,
            @NotNull(message = "請選擇角色") Role role,
            @NotNull(message = "請選擇狀態") AccountStatus status) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "密碼不可為空") @Size(min = 8, max = 100, message = "密碼長度需為 8~100 碼") String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "請輸入目前密碼") String currentPassword,
            @NotBlank(message = "密碼不可為空") @Size(min = 8, max = 100, message = "密碼長度需為 8~100 碼") String newPassword) {
    }

    /** 目前登入的後台帳號(前端依角色決定顯示哪些選單) */
    @GetMapping("/api/admin/account/me")
    public ApiResponse<AdminAccountService.AccountResponse> me() {
        return ApiResponse.success(accountService.me(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/api/admin/account/password")
    public ApiResponse<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changeOwnPassword(SecurityUtils.getCurrentUserId(), request.currentPassword(),
                request.newPassword());
        return ApiResponse.success("密碼已更新", null);
    }

    @GetMapping("/api/admin/accounts")
    public ApiResponse<List<AdminAccountService.AccountResponse>> list() {
        return ApiResponse.success(accountService.list());
    }

    @AdminAudit(action = "新增後台帳號", target = AuditTarget.ACCOUNT, detail = "#request.username + ' ' + #request.role")
    @PostMapping("/api/admin/accounts")
    public ApiResponse<AdminAccountService.AccountResponse> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success("帳號已建立",
                accountService.create(request.username(), request.name(), request.password(), request.role()));
    }

    @AdminAudit(action = "修改後台帳號", target = AuditTarget.ACCOUNT, detail = "#request.role + ' ' + #request.status")
    @PutMapping("/api/admin/accounts/{id}")
    public ApiResponse<AdminAccountService.AccountResponse> update(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.success("已更新", accountService.update(SecurityUtils.getCurrentUserId(), id,
                request.name(), request.role(), request.status()));
    }

    @AdminAudit(action = "重設後台帳號密碼", target = AuditTarget.ACCOUNT)
    @PutMapping("/api/admin/accounts/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(id, request.newPassword());
        return ApiResponse.success("密碼已重設", null);
    }
}
