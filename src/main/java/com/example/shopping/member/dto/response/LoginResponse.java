package com.example.shopping.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private String refreshToken;
    private Long memberId;
    private String name;
    private String email;
    /** 這次登入的工作階段 id(登入裝置管理用來標示「目前這台裝置」) */
    private String sessionId;

    public static LoginResponse of(String token, String refreshToken, Long memberId, String name, String email,
                                   String sessionId) {
        return new LoginResponse(token, "Bearer", refreshToken, memberId, name, email, sessionId);
    }
}
