package com.example.shopping.points.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 管理員調整會員購物金:正數為發放、負數為扣除 */
@Getter
@Setter
public class PointAdjustRequest {

    @NotNull(message = "請輸入調整點數")
    @Min(value = -100_000, message = "單次最多扣除 100000 點")
    @Max(value = 100_000, message = "單次最多發放 100000 點")
    private Integer amount;

    @NotBlank(message = "請輸入調整原因")
    @Size(max = 100, message = "調整原因最多 100 字")
    private String reason;
}
