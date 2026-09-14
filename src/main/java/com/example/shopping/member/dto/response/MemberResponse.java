package com.example.shopping.member.dto.response;

import com.example.shopping.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MemberResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private LocalDateTime createdAt;

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getCreatedAt());
    }
}
