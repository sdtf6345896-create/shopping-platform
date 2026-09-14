package com.example.shopping.member.dto.request;

import com.example.shopping.common.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberStatusRequest {

    @NotNull(message = "狀態不可為空")
    private AccountStatus status;
}
