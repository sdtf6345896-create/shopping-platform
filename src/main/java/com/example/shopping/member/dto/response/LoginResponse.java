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

    public static LoginResponse of(String token, String refreshToken, Long memberId, String name, String email) {
        return new LoginResponse(token, "Bearer", refreshToken, memberId, name, email);
    }
}
