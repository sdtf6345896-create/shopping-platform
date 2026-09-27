package com.example.shopping.returns.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnApplyRequest {

    @NotBlank(message = "請填寫退貨原因")
    @Size(max = 500, message = "退貨原因最多 500 字")
    private String reason;
}
