package com.example.shopping.returns.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnDecisionRequest {

    /** 處理說明(拒絕時建議填寫原因,會寄給會員) */
    @Size(max = 255, message = "處理說明最多 255 字")
    private String note;
}
