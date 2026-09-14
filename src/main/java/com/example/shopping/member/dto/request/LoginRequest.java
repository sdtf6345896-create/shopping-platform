package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email 不可為空")
    private String email;

    @NotBlank(message = "密碼不可為空")
    private String password;
}
