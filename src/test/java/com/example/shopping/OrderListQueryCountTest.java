package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.dto.request.AdminOrderQuery;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.order.service.OrderService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防止訂單列表出現 N+1 查詢:每筆訂單都有明細、狀態歷程、退貨申請等延遲載入的關聯,
 * 透過 hibernate.default_batch_fetch_size 批次載入,SQL 數量不應隨筆數線性成長。
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class OrderListQueryCountTest {

    private static final int ORDER_COUNT = 10;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String keyword;

    @BeforeEach
    void seedOrders() {
        keyword = "qc-" + UUID.randomUUID().toString().substring(0, 8);

        Member member = new Member();
        member.setEmail(keyword + "@example.com");
        member.setPassword("x");
        member.setName("查詢測試");
        member.setEmailVerified(true);
        memberRepository.save(member);

        Category category = new Category();
        category.setName(keyword);
        categoryRepository.save(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("查詢測試商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode(keyword);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(100);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);

        for (int i = 0; i < ORDER_COUNT; i++) {
            Orders order = new Orders();
            order.setOrderNo(keyword + "-" + i);
            order.setMember(member);
            order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            order.markCreated(OrderActor.MEMBER);
            order.changeStatus(OrderStatus.PAID, OrderActor.MEMBER, null);
            order.setReceiverName("王小明");
            order.setReceiverPhone("0912345678");
            order.setReceiverAddress("台北市");
            order.setSubtotalAmount(new BigDecimal("200"));
            order.setTotalAmount(new BigDecimal("200"));
            for (int j = 0; j < 2; j++) {
                OrderItem item = new OrderItem();
                item.setProductSku(sku);
                item.setProductName(product.getName());
                item.setSpecName(sku.getSpecName());
                item.setUnitPrice(new BigDecimal("100"));
                item.setQuantity(1);
                item.setSubtotal(new BigDecimal("100"));
                order.addItem(item);
            }
            orderRepository.save(order);
        }
    }

    @Test
    void adminOrderList_loadsAssociationsInBatches() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        AdminOrderQuery query = new AdminOrderQuery();
        query.setKeyword(keyword);
        var page = orderService.listAdmin(query, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(ORDER_COUNT);
        // 修正前為 31 條(每筆訂單各查明細、歷程、退貨申請);批次載入後為 4 條:訂單 + 三種關聯各一次 IN 查詢
        assertThat(statistics.getPrepareStatementCount())
                .as("SQL statements for listing %d orders", ORDER_COUNT)
                .isLessThanOrEqualTo(6);
    }
}
