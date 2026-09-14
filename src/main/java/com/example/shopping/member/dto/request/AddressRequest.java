package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "收件人姓名不可為空")
    private String recipientName;

    @NotBlank(message = "電話不可為空")
    private String phone;

    private String postalCode;

    @NotBlank(message = "縣市不可為空")
    private String city;

    @NotBlank(message = "鄉鎮區不可為空")
    private String district;

    @NotBlank(message = "詳細地址不可為空")
    private String detailAddress;

    // 注意:欄位刻意不用 isXxx 命名 — Lombok 對 "isDefault" 欄位會產生 setDefault()/isDefault(),
    // Jackson 因此會把 JSON 屬性解讀成 "default" 而非 "isDefault",導致前端傳的欄位被忽略。
    private boolean defaultAddress;
}
