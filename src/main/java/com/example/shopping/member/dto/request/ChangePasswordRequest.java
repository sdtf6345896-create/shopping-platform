package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "請輸入目前密碼")
    private String currentPassword;

    @NotBlank(message = "新密碼不可為空")
    @Size(min = 8, message = "密碼長度至少 8 碼")
    private String newPassword;
}
