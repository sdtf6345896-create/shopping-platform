package com.example.shopping;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.order.service.OrderService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 最後一件商品被多人同時結帳時,只能有一人成功,庫存不可變成負數或被超賣。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentCheckoutTest {

    private static final int BUYERS = 5;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Test
    void lastUnit_canOnlyBeSoldOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ProductSku sku = createSkuWithStock(suffix, 1);

        List<Callable<Long>> checkouts = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            checkouts.add(buyerCheckingOut(suffix + "-" + i, sku));
        }

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();
        for (Callable<Long> checkout : checkouts) {
            results.add(pool.submit(() -> {
                start.await();
                return checkout.call();
            }));
        }
        start.countDown();

        int succeeded = 0;
        int soldOut = 0;
        for (Future<Long> result : results) {
            try {
                result.get();
                succeeded++;
            } catch (java.util.concurrent.ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOf(BusinessException.class).hasMessageContaining("庫存不足");
                soldOut++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).as("只能有一位買家成功").isEqualTo(1);
        assertThat(soldOut).isEqualTo(BUYERS - 1);
        assertThat(productSkuRepository.findById(sku.getId()).orElseThrow().getStock()).isZero();
    }

    private ProductSku createSkuWithStock(String suffix, int stock) {
        Category category = new Category();
        category.setName("併發測試-" + suffix);
        categoryRepository.save(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("限量商品");
        product.setPrice(new BigDecimal("100"));
        product.setStatus(ProductStatus.ON_SALE);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("LIMITED-" + suffix);
        sku.setSpecName("唯一一件");
        sku.setPrice(new BigDecimal("100"));
        sku.setStock(stock);
        product.replaceSkus(List.of(sku));
        productRepository.save(product);
        return product.getSkus().get(0);
    }

    /** 建立一位購物車裡有該商品的會員,回傳「結帳」這個動作 */
    private Callable<Long> buyerCheckingOut(String name, ProductSku sku) {
        Member member = new Member();
        member.setEmail(name + "@example.com");
        member.setPassword("x");
        member.setName(name);
        member.setEmailVerified(true);
        memberRepository.save(member);

        Address address = new Address();
        address.setMember(member);
        address.setRecipientName(name);
        address.setPhone("0912345678");
        address.setCity("台北市");
        address.setDistrict("大安區");
        address.setDetailAddress("復興南路一段1號");
        addressRepository.save(address);

        CartItem cartItem = new CartItem();
        cartItem.setMember(member);
        cartItem.setProductSku(sku);
        cartItem.setQuantity(1);
        cartItemRepository.save(cartItem);

        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId(address.getId());
        request.setPaymentMethod(PaymentMethod.COD);
        return () -> orderService.checkout(member.getId(), request).getId();
    }
}
