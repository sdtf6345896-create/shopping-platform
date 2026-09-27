package com.example.shopping.product.service;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.product.dto.response.StockImportResponse;
import com.example.shopping.product.dto.response.StockImportResponse.RowError;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    public StockImportResponse importCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("請選擇 CSV 檔案");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException("檔案過大,請控制在 1MB 以內");
        }

        List<RowError> errors = new ArrayList<>();
        int totalRows = 0;
        // skuCode -> (行號, 新庫存),保留檔案順序
        Map<String, int[]> rows = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) {
                    line = stripBom(line);
                    if (isHeader(line)) {
                        continue;
                    }
                }
                if (line.isBlank()) {
                    continue;
                }
                if (++totalRows > MAX_ROWS) {
                    throw new BusinessException("一次最多匯入 " + MAX_ROWS + " 筆");
                }
                parseRow(line, lineNo, rows, errors);
            }
        } catch (IOException e) {
            throw new BusinessException("無法讀取檔案,請確認為 UTF-8 編碼的 CSV");
        }
        if (totalRows == 0) {
            throw new BusinessException("檔案中沒有資料");
        }

        Map<String, ProductSku> skus = productSkuRepository.findBySkuCodeIn(rows.keySet()).stream()
                .collect(Collectors.toMap(ProductSku::getSkuCode, Function.identity()));
        rows.forEach((code, value) -> {
            if (!skus.containsKey(code)) {
                errors.add(new RowError(value[0], "找不到 SKU:" + code));
            }
        });

        if (!errors.isEmpty()) {
            errors.sort((a, b) -> Integer.compare(a.line(), b.line()));
            return new StockImportResponse(false, totalRows, 0, errors);
        }
        rows.forEach((code, value) -> skus.get(code).setStock(value[1]));
        return new StockImportResponse(true, totalRows, rows.size(), List.of());
    }

    private static void parseRow(String line, int lineNo, Map<String, int[]> rows, List<RowError> errors) {
        String[] cells = line.split(",", -1);
        if (cells.length < 2) {
            errors.add(new RowError(lineNo, "格式應為:SKU 編號,庫存"));
            return;
        }
        String code = unquote(cells[0]);
        String stockText = unquote(cells[1]);
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

    private static String stripBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }

    private static String unquote(String cell) {
        String trimmed = cell.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
