package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateRequest {

    @NotBlank(message = "姓名不可為空")
    private String name;

    private String phone;
}
