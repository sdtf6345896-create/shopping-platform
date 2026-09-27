package com.example.shopping;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.category.entity.Category;
import com.example.shopping.category.repository.CategoryRepository;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多人同時結帳搶最後一件商品 / 最後一張限量優惠券時,只能有一人成功,不可超賣或超發。
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
    @Autowired
    private CouponRepository couponRepository;

    @Test
    void lastUnit_canOnlyBeSoldOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ProductSku sku = createSkuWithStock(suffix, 1);

        List<Callable<Long>> checkouts = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            checkouts.add(buyerCheckingOut(suffix + "-" + i, sku));
        }

        int succeeded = runConcurrently(checkouts, "庫存不足");

        assertThat(succeeded).as("只能有一位買家成功").isEqualTo(1);
        assertThat(productSkuRepository.findById(sku.getId()).orElseThrow().getStock()).isZero();
    }

    @Test
    void lastCouponUse_canOnlyBeClaimedOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Coupon coupon = new Coupon();
        coupon.setCode("LAST" + suffix.toUpperCase());
        coupon.setName("限量一張");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setMinSpendAmount(BigDecimal.ZERO);
        coupon.setTotalQuantity(1);
        couponRepository.save(coupon);

        List<Callable<Long>> checkouts = new ArrayList<>();
        for (int i = 0; i < BUYERS; i++) {
            // 每人買不同商品:共用同一個 SKU 時,扣庫存的列鎖會讓交易排隊,測不出優惠券本身的競爭
            ProductSku sku = createSkuWithStock(suffix + "-c" + i, 100);
            checkouts.add(buyerCheckingOut(suffix + "-c" + i, sku, coupon.getCode()));
        }

        int succeeded = runConcurrently(checkouts, "兌換完畢");

        assertThat(succeeded).as("限量一張的優惠券只能被用一次").isEqualTo(1);
        assertThat(couponRepository.findById(coupon.getId()).orElseThrow().getUsedQuantity()).isEqualTo(1);
    }

    /** 同時執行所有結帳,回傳成功筆數;失敗的必須是預期的業務錯誤 */
    private int runConcurrently(List<Callable<Long>> checkouts, String expectedError) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(checkouts.size());
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
        for (Future<Long> result : results) {
            try {
                result.get();
                succeeded++;
            } catch (ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOf(BusinessException.class).hasMessageContaining(expectedError);
            }
        }
        pool.shutdown();
        return succeeded;
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
        return buyerCheckingOut(name, sku, null);
    }

    private Callable<Long> buyerCheckingOut(String name, ProductSku sku, String couponCode) {
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
        request.setCouponCode(couponCode);
        return () -> orderService.checkout(member.getId(), request).getId();
    }
}
