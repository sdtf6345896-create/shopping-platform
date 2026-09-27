package com.example.shopping;

import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.recommendation.dto.BoughtTogetherResponse;
import com.example.shopping.recommendation.service.BoughtTogetherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 「買了這個的人也買了」依真實訂單統計,排除未付款 / 取消的訂單、本商品與下架商品 */
@SpringBootTest
@ActiveProfiles("test")
class BoughtTogetherIntegrationTest {

    @Autowired
    private BoughtTogetherService boughtTogetherService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    private String suffix;
    private Member member;
    private Category category;

    private Product product(String name, ProductStatus status) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setPrice(new BigDecimal("100"));
        product.setStatus(status);
        ProductSku sku = new ProductSku();
        sku.setSkuCode(name + "-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(100);
        product.replaceSkus(List.of(sku));
        return productRepository.save(product);
    }

    private void order(OrderStatus status, Product... products) {
        Orders order = new Orders();
        order.setOrderNo("BT-" + UUID.randomUUID().toString().substring(0, 12));
        order.setMember(member);
        order.setPaymentMethod(PaymentMethod.COD);
        order.markCreated(OrderActor.MEMBER);
        if (status != OrderStatus.PENDING_PAYMENT) {
            order.changeStatus(status, OrderActor.SYSTEM, null);
        }
        order.setReceiverName("王小明");
        order.setReceiverPhone("0912345678");
        order.setReceiverAddress("台北市");
        order.setSubtotalAmount(new BigDecimal("100"));
        order.setTotalAmount(new BigDecimal("100"));
        for (Product product : products) {
            OrderItem item = new OrderItem();
            item.setProductSku(product.getSkus().get(0));
            item.setProductName(product.getName());
            item.setSpecName("標準");
            item.setUnitPrice(new BigDecimal("100"));
            item.setQuantity(1);
            item.setSubtotal(new BigDecimal("100"));
            order.addItem(item);
        }
        orderRepository.save(order);
    }

    @Test
    void ranksProductsByNumberOfOrdersBoughtTogether() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        member = new Member();
        member.setEmail("bt-" + suffix + "@example.com");
        member.setPassword("x");
        member.setName("共同購買");
        member.setEmailVerified(true);
        memberRepository.save(member);
        category = new Category();
        category.setName("共同購買-" + suffix);
        categoryRepository.save(category);

        Product camera = product("相機", ProductStatus.ON_SALE);
        Product memoryCard = product("記憶卡", ProductStatus.ON_SALE);
        Product tripod = product("腳架", ProductStatus.ON_SALE);
        Product discontinued = product("停產鏡頭", ProductStatus.OFF_SHELF);
        Product unpaidOnly = product("只在未付款訂單", ProductStatus.ON_SALE);

        order(OrderStatus.COMPLETED, camera, memoryCard, tripod);
        order(OrderStatus.PAID, camera, memoryCard);
        order(OrderStatus.SHIPPING, camera, memoryCard, discontinued);
        order(OrderStatus.PENDING_PAYMENT, camera, unpaidOnly);
        order(OrderStatus.CANCELLED, camera, tripod);

        List<BoughtTogetherResponse> result = boughtTogetherService.boughtTogether(camera.getId(), 6);

        assertThat(result).extracting(r -> r.product().getName()).containsExactly("記憶卡", "腳架");
        assertThat(result).extracting(BoughtTogetherResponse::orderCount).containsExactly(3L, 1L);
    }
}
