package com.example.shopping.report.dto;

import java.math.BigDecimal;

/**
 * 頂層分類的銷售彙總(子分類的銷售併入其頂層分類)。
 *
 * @param share 營收佔比(百分比,小數一位)
 */
public record CategorySalesResponse(Long categoryId, String categoryName, long soldQuantity, BigDecimal revenue,
                                    BigDecimal share) {
}
