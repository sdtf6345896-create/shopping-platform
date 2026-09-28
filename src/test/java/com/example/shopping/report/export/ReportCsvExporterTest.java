package com.example.shopping.report.export;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.report.dto.CategorySalesResponse;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.TopProductResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportCsvExporterTest {

    private static List<String> lines(byte[] csv) {
        // 去掉 3 bytes 的 UTF-8 BOM,依 CRLF 切行
        return List.of(new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8).split("\r\n"));
    }

    @Test
    void daily_writesOneRowPerDay() {
        List<String> lines = lines(ReportCsvExporter.daily(List.of(
                new DailySalesResponse(LocalDate.of(2026, 9, 27), 3, new BigDecimal("1580.00")),
                new DailySalesResponse(LocalDate.of(2026, 9, 28), 0, BigDecimal.ZERO))));

        assertThat(lines).containsExactly("日期,訂單數,營收", "2026-09-27,3,1580.00", "2026-09-28,0,0");
    }

    @Test
    void products_numbersRanksAndEscapesNames() {
        List<String> lines = lines(ReportCsvExporter.products(List.of(
                new TopProductResponse(7L, "T恤, 黑色", "/img.jpg", 12, new BigDecimal("7080")),
                new TopProductResponse(3L, "=HACK()", null, 5, new BigDecimal("500")))));

        assertThat(lines).containsExactly("排名,商品編號,商品名稱,銷售數量,營收",
                "1,7,\"T恤, 黑色\",12,7080", "2,3,'=HACK(),5,500");
    }

    @Test
    void categories_includesShare() {
        List<String> lines = lines(ReportCsvExporter.categories(List.of(
                new CategorySalesResponse(1L, "女裝", 10, new BigDecimal("8000"), new BigDecimal("80.0")))));

        assertThat(lines).containsExactly("分類,銷售數量,營收,營收占比(%)", "女裝,10,8000,80.0");
    }

    @Test
    void parseType_isCaseInsensitive_andRejectsUnknown() {
        assertThat(ReportCsvExporter.parseType(" Daily ")).isEqualTo(ReportCsvExporter.Type.DAILY);
        assertThatThrownBy(() -> ReportCsvExporter.parseType("orders")).isInstanceOf(BusinessException.class);
    }
}
