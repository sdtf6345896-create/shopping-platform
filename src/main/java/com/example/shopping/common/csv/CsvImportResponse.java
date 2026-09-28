package com.example.shopping.common.csv;

import java.util.List;

/**
 * CSV 批次匯入結果。有任何錯誤時 applied 為 false,且完全沒有更新(全有或全無)。
 *
 * @param totalRows 資料列數(不含標題列與空白列)
 */
public record CsvImportResponse(boolean applied, int totalRows, int updated, List<RowError> errors) {

    public static CsvImportResponse rejected(int totalRows, List<RowError> errors) {
        List<RowError> sorted = errors.stream().sorted((a, b) -> Integer.compare(a.line(), b.line())).toList();
        return new CsvImportResponse(false, totalRows, 0, sorted);
    }

    public record RowError(int line, String message) {
    }
}
