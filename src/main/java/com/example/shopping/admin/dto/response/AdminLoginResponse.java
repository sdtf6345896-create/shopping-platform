package com.example.shopping.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;
    private String tokenType;
    private Long adminId;
    private String username;
    private String name;

    public static AdminLoginResponse of(String token, Long adminId, String username, String name) {
        return new AdminLoginResponse(token, "Bearer", adminId, username, name);
    }
}
