package com.example.shopping;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.wishlist.entity.WishlistItem;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 會員刪除帳號:有處理中訂單時被擋;沒有時匿名化並清除購物車、收藏,舊帳密無法再登入 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    private MockHttpServletResponse send(MockHttpServletRequestBuilder request,
                                         String token, Object body) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        return mockMvc.perform(request).andReturn().getResponse();
    }

    private String login(String email) throws Exception {
        MockHttpServletResponse response = send(post("/api/auth/login"), null,
                Map.of("email", email, "password", "password123"));
        assertThat(response.getStatus()).isEqualTo(200);
        return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8)).at("/data/token").asText();
    }

    @Test
    void deleteAccount_blockedByOpenOrder_thenSucceedsAndCleansUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "bye-" + suffix + "@example.com";
        Member member = new Member();
        member.setEmail(email);
        member.setPassword(passwordEncoder.encode("password123"));
        member.setName("要離開的會員");
        member.setEmailVerified(true);
        memberRepository.save(member);

        Category category = new Category();
        category.setName("刪帳號-" + suffix);
        categoryRepository.save(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName("刪帳號測試商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("BYE-" + suffix);
        sku.setSpecName("標準");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(5);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);

        CartItem cartItem = new CartItem();
        cartItem.setMember(member);
        cartItem.setProductSku(product.getSkus().get(0));
        cartItem.setQuantity(1);
        cartItemRepository.save(cartItem);
        WishlistItem wish = new WishlistItem();
        wish.setMember(member);
        wish.setProduct(product);
        wishlistItemRepository.save(wish);

        Orders order = new Orders();
        order.setOrderNo("BYE-" + suffix);
        order.setMember(member);
        order.setPaymentMethod(PaymentMethod.COD);
        order.markCreated(OrderActor.MEMBER);
        order.setReceiverName("要離開的會員");
        order.setReceiverPhone("0912345678");
        order.setReceiverAddress("台北市");
        order.setSubtotalAmount(new BigDecimal("100"));
        order.setTotalAmount(new BigDecimal("100"));
        orderRepository.save(order);

        String token = login(email);

        // 還有待付款訂單 → 不能刪
        MockHttpServletResponse blocked = send(delete("/api/members/me"), token, Map.of("password", "password123"));
        assertThat(blocked.getStatus()).isEqualTo(400);
        assertThat(blocked.getContentAsString(StandardCharsets.UTF_8)).contains("處理中的訂單");

        // 密碼錯誤 → 不能刪
        assertThat(send(delete("/api/members/me"), token, Map.of("password", "wrong-pass")).getStatus()).isEqualTo(400);

        order.changeStatus(OrderStatus.CANCELLED, OrderActor.MEMBER, null);
        orderRepository.save(order);

        MockHttpServletResponse deleted = send(delete("/api/members/me"), token, Map.of("password", "password123"));
        assertThat(deleted.getStatus()).isEqualTo(200);

        Member anonymized = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(anonymized.getEmail()).startsWith("deleted-");
        assertThat(anonymized.getName()).isEqualTo("已刪除會員");
        assertThat(cartItemRepository.findById(cartItem.getId())).isEmpty();
        assertThat(wishlistItemRepository.findById(wish.getId())).isEmpty();
        // 訂單保留(帳務紀錄)
        assertThat(orderRepository.findById(order.getId())).isPresent();
        // 舊的 email / 密碼無法再登入
        assertThat(send(post("/api/auth/login"), null, Map.of("email", email, "password", "password123")).getStatus())
                .isEqualTo(400);
    }
}
