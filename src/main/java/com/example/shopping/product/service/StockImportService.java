package com.example.shopping.product.service;

import com.example.shopping.common.csv.CsvImportReader;
import com.example.shopping.common.csv.CsvImportResponse;
import com.example.shopping.common.csv.CsvImportResponse.RowError;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 以 CSV(SKU 編號, 庫存)批次設定庫存。先驗證全部資料列,有任何錯誤就完全不更新並回報每一列的問題。
 */
@Service
@Transactional
public class StockImportService {

    static final int MAX_ROWS = 5000;
    static final long MAX_FILE_BYTES = 1024 * 1024;
    static final int MAX_STOCK = 1_000_000;

    private final ProductSkuRepository productSkuRepository;

    public StockImportService(ProductSkuRepository productSkuRepository) {
        this.productSkuRepository = productSkuRepository;
    }

    public CsvImportResponse importCsv(MultipartFile file) {
        List<CsvImportReader.Row> csvRows =
                CsvImportReader.read(file, MAX_ROWS, MAX_FILE_BYTES, StockImportService::isHeader);

        List<RowError> errors = new ArrayList<>();
        // skuCode -> (行號, 新庫存),保留檔案順序
        Map<String, int[]> rows = new LinkedHashMap<>();
        for (CsvImportReader.Row row : csvRows) {
            parseRow(row, rows, errors);
        }

        Map<String, ProductSku> skus = productSkuRepository.findBySkuCodeIn(rows.keySet()).stream()
                .collect(Collectors.toMap(ProductSku::getSkuCode, Function.identity()));
        rows.forEach((code, value) -> {
            if (!skus.containsKey(code)) {
                errors.add(new RowError(value[0], "找不到 SKU:" + code));
            }
        });

        if (!errors.isEmpty()) {
            return CsvImportResponse.rejected(csvRows.size(), errors);
        }
        rows.forEach((code, value) -> skus.get(code).setStock(value[1]));
        return new CsvImportResponse(true, csvRows.size(), rows.size(), List.of());
    }

    private static void parseRow(CsvImportReader.Row row, Map<String, int[]> rows, List<RowError> errors) {
        int lineNo = row.line();
        if (row.cells().size() < 2) {
            errors.add(new RowError(lineNo, "格式應為:SKU 編號,庫存"));
            return;
        }
        String code = row.cell(0);
        String stockText = row.cell(1);
        if (code.isEmpty()) {
            errors.add(new RowError(lineNo, "SKU 編號不可為空"));
            return;
        }
        int stock;
        try {
            stock = Integer.parseInt(stockText);
        } catch (NumberFormatException e) {
            errors.add(new RowError(lineNo, "庫存必須是整數:" + stockText));
            return;
        }
        if (stock < 0 || stock > MAX_STOCK) {
            errors.add(new RowError(lineNo, "庫存需介於 0 到 " + MAX_STOCK + " 之間"));
            return;
        }
        int[] previous = rows.put(code, new int[]{lineNo, stock});
        if (previous != null) {
            errors.add(new RowError(lineNo, "SKU 重複(第 " + previous[0] + " 行已出現):" + code));
        }
    }

    private static boolean isHeader(String line) {
        String lower = line.toLowerCase();
        return lower.contains("sku") && !lower.matches(".*,\\s*\"?\\d+\"?\\s*$");
    }
}
