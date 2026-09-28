package com.example.shopping.report.export;

import com.example.shopping.common.csv.CsvWriter;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.report.dto.CategorySalesResponse;
import com.example.shopping.report.dto.DailySalesResponse;
import com.example.shopping.report.dto.TopProductResponse;
import com.example.shopping.report.service.ReportService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/** 後台報表匯出 CSV:與報表頁相同的查詢區間與計算方式 */
@Component
public class ReportCsvExporter {

    /** 熱銷商品匯出的筆數上限(報表頁只顯示前 10 名) */
    static final int TOP_PRODUCTS_LIMIT = 100;

    public enum Type {
        DAILY, PRODUCTS, CATEGORIES
    }

    private final ReportService reportService;

    public ReportCsvExporter(ReportService reportService) {
        this.reportService = reportService;
    }

    public static Type parseType(String type) {
        try {
            return Type.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("不支援的報表類型:" + type);
        }
    }

    public byte[] export(Type type, LocalDate startDate, LocalDate endDate) {
        return switch (type) {
            case DAILY -> daily(reportService.getDailySales(startDate, endDate));
            case PRODUCTS -> products(reportService.getTopProducts(startDate, endDate, TOP_PRODUCTS_LIMIT));
            case CATEGORIES -> categories(reportService.getCategorySales(startDate, endDate));
        };
    }

    static byte[] daily(List<DailySalesResponse> rows) {
        return CsvWriter.write(List.of("日期", "訂單數", "營收"), rows.stream()
                .map(r -> List.of(r.getDate().toString(), String.valueOf(r.getOrderCount()), r.getRevenue().toPlainString()))
                .toList());
    }

    static byte[] products(List<TopProductResponse> rows) {
        return CsvWriter.write(List.of("排名", "商品編號", "商品名稱", "銷售數量", "營收"),
                IntStream.range(0, rows.size())
                        .mapToObj(i -> {
                            TopProductResponse r = rows.get(i);
                            return List.of(String.valueOf(i + 1), String.valueOf(r.getProductId()), r.getProductName(),
                                    String.valueOf(r.getSoldQuantity()), r.getRevenue().toPlainString());
                        })
                        .toList());
    }

    static byte[] categories(List<CategorySalesResponse> rows) {
        return CsvWriter.write(List.of("分類", "銷售數量", "營收", "營收占比(%)"), rows.stream()
                .map(r -> List.of(r.categoryName(), String.valueOf(r.soldQuantity()), r.revenue().toPlainString(),
                        r.share().toPlainString()))
                .toList());
    }
}
