package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "重設密碼 token 不可為空")
    private String token;

    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, message = "密碼長度至少 8 碼")
    private String newPassword;
}
