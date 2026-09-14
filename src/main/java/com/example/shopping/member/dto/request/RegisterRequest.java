package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, message = "密碼長度至少 8 碼")
    private String password;

    @NotBlank(message = "姓名不可為空")
    private String name;

    private String phone;
}
