package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** sitemap.xml / robots.txt 不需登入;只收錄上架商品,網址為絕對網址且 & 有跳脫 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private Product product(Category category, ProductStatus status) {
        Product product = new Product();
        product.setCategory(category);
        product.setName("SEO 商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(status);
        return productRepository.save(product);
    }

    @Test
    void sitemapListsOnSaleProductsAndActiveCategories() throws Exception {
        Category category = new Category();
        category.setName("SEO 分類");
        categoryRepository.save(category);
        Product onSale = product(category, ProductStatus.ON_SALE);
        Product offShelf = product(category, ProductStatus.OFF_SHELF);

        String xml = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andReturn().getResponse().getContentAsString();

        assertThat(xml).startsWith("<?xml");
        assertThat(xml).contains("<loc>http://localhost:5173/products/" + onSale.getId() + "</loc>");
        assertThat(xml).doesNotContain("/products/" + offShelf.getId() + "<");
        assertThat(xml).contains("<loc>http://localhost:5173/products?categoryId=" + category.getId() + "</loc>");
    }

    @Test
    void robotsPointsToSitemapAndHidesPrivatePages() throws Exception {
        String robots = mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(robots).contains("Sitemap: http://localhost:5173/sitemap.xml");
        assertThat(robots).contains("Disallow: /admin").contains("Disallow: /checkout");
    }
}
