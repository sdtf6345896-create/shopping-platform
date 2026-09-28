package com.example.shopping.common.csv;

import com.example.shopping.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 後台 CSV 匯入共用的讀檔:UTF-8(可含 BOM)、略過標題列與空白列、限制檔案大小與筆數。
 * 儲存格可用雙引號包住(內含逗號、"" 代表一個引號),但不支援跨行的儲存格。
 */
public final class CsvImportReader {

    private CsvImportReader() {
    }

    public record Row(int line, List<String> cells) {

        /** 第 index 格(已去頭尾空白與外層引號),不存在時回空字串 */
        public String cell(int index) {
            return index < cells.size() ? cells.get(index) : "";
        }
    }

    /**
     * @param isHeader 判斷第一行是否為標題列
     * @throws BusinessException 檔案為空、過大、筆數過多或無法讀取
     */
    public static List<Row> read(MultipartFile file, int maxRows, long maxBytes, Predicate<String> isHeader) {
        if (file.isEmpty()) {
            throw new BusinessException("請選擇 CSV 檔案");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("檔案過大,請控制在 " + (maxBytes / 1024) + "KB 以內");
        }
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) {
                    line = stripBom(line);
                    if (isHeader.test(line)) {
                        continue;
                    }
                }
                if (line.isBlank()) {
                    continue;
                }
                if (rows.size() >= maxRows) {
                    throw new BusinessException("一次最多匯入 " + maxRows + " 筆");
                }
                rows.add(new Row(lineNo, splitCells(line)));
            }
        } catch (IOException e) {
            throw new BusinessException("無法讀取檔案,請確認為 UTF-8 編碼的 CSV");
        }
        if (rows.isEmpty()) {
            throw new BusinessException("檔案中沒有資料");
        }
        return rows;
    }

    static List<String> splitCells(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"' && cell.toString().isBlank()) {
                cell.setLength(0);
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString().trim());
        return cells;
    }

    private static String stripBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }
}
