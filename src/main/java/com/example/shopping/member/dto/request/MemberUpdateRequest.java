package com.example.shopping.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MemberUpdateRequest {

    @NotBlank(message = "姓名不可為空")
    private String name;

    private String phone;

    /** 生日;只能設定一次,已設定時需與原值相同(null 表示不變) */
    @Past(message = "生日必須是過去的日期")
    private LocalDate birthday;
}
