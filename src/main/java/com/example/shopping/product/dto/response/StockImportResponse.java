package com.example.shopping.product.dto.response;

import java.util.List;

/**
 * 庫存匯入結果。有任何錯誤時 applied 為 false,且完全沒有更新(全有或全無)。
 *
 * @param totalRows 資料列數(不含標題列與空白列)
 */
public record StockImportResponse(boolean applied, int totalRows, int updated, List<RowError> errors) {

    public record RowError(int line, String message) {
    }
}
