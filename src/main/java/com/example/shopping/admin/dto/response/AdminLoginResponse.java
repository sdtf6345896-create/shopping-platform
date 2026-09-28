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
    /** ADMIN 或 STAFF */
    private String role;

    public static AdminLoginResponse of(String token, Long adminId, String username, String name, String role) {
        return new AdminLoginResponse(token, "Bearer", adminId, username, name, role);
    }
}
