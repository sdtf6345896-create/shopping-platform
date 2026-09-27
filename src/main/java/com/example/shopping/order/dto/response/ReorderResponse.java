package com.example.shopping.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 「再買一次」結果:成功加入購物車的品項數,以及無法(完整)加入的品項說明 */
@Getter
@AllArgsConstructor
public class ReorderResponse {

    private int addedCount;
    private List<String> notices;
}
