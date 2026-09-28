package com.example.shopping.seo;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.CategoryStatus;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

/**
 * 給搜尋引擎的 sitemap.xml 與 robots.txt(依上架商品與啟用分類即時產生)。
 * 網址一律用 app.site.base-url 組成絕對網址;會員中心、購物車、後台等不需要被收錄的頁面在 robots.txt 排除。
 */
@RestController
public class SeoController {

    /** 單一 sitemap 檔案的網址上限(sitemaps.org 規範) */
    static final int MAX_URLS = 50_000;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final String baseUrl;

    public SeoController(ProductRepository productRepository,
                         CategoryRepository categoryRepository,
                         @Value("${app.site.base-url}") String baseUrl) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public String sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        int count = 0;
        count += appendUrl(xml, "/", null, "daily");
        count += appendUrl(xml, "/products", null, "daily");
        for (Category category : categoryRepository.findByStatusOrderBySortOrderAscIdAsc(CategoryStatus.ACTIVE)) {
            count += appendUrl(xml, "/products?categoryId=" + category.getId(), null, "weekly");
        }
        for (ProductRepository.SitemapEntry product : productRepository.findSitemapEntries(ProductStatus.ON_SALE)) {
            if (count >= MAX_URLS) {
                break;
            }
            String lastmod = product.getUpdatedAt() == null ? null
                    : product.getUpdatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            count += appendUrl(xml, "/products/" + product.getId(), lastmod, "weekly");
        }
        return xml.append("</urlset>\n").toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return String.join("\n",
                "User-agent: *",
                "Disallow: /admin",
                "Disallow: /member",
                "Disallow: /cart",
                "Disallow: /checkout",
                "Disallow: /orders",
                "Disallow: /api/",
                "",
                "Sitemap: " + baseUrl + "/sitemap.xml",
                "");
    }

    private int appendUrl(StringBuilder xml, String path, String lastmod, String changefreq) {
        xml.append("  <url><loc>").append(escape(baseUrl + path)).append("</loc>");
        if (lastmod != null) {
            xml.append("<lastmod>").append(lastmod).append("</lastmod>");
        }
        xml.append("<changefreq>").append(changefreq).append("</changefreq></url>\n");
        return 1;
    }

    /** XML 跳脫(網址中的 & 必須寫成 &amp;) */
    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
