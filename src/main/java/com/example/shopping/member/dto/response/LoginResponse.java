package com.example.shopping.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long memberId;
    private String name;
    private String email;

    public static LoginResponse of(String token, Long memberId, String name, String email) {
        return new LoginResponse(token, "Bearer", memberId, name, email);
    }
}
