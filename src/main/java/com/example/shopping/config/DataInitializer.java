package com.example.shopping.config;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.entity.OrderItem;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 開發環境用種子資料:
 * 1. 若無任何管理員帳號,建立預設帳密方便登入測試(admin / admin123)。
 * 2. 若無展示用會員,建立一組會員 + 地址 + 一批跨越近 20 天、狀態各異的歷史訂單,
 *    讓後台訂單管理 / 銷售報表在剛啟動專案時就有內容可看,而不是空畫面。
 *    訂單編號刻意用 "DEMO" 開頭,跟正式下單產生的 "ORD..." 區隔,一眼就能看出是展示資料。
 */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_MEMBER_EMAIL = "demo@momoshop.test";

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;
    private final ProductSkuRepository productSkuRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(AdminRepository adminRepository,
                            MemberRepository memberRepository,
                            AddressRepository addressRepository,
                            ProductSkuRepository productSkuRepository,
                            OrderRepository orderRepository,
                            PasswordEncoder passwordEncoder,
                            JdbcTemplate jdbcTemplate) {
        this.adminRepository = adminRepository;
        this.memberRepository = memberRepository;
        this.addressRepository = addressRepository;
        this.productSkuRepository = productSkuRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedDemoMemberAndOrders();
    }

    private void seedAdmin() {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("系統管理員");
            adminRepository.save(admin);
        }
    }

    private void seedDemoMemberAndOrders() {
        if (memberRepository.findByEmail(DEMO_MEMBER_EMAIL).isPresent()) {
            return;
        }

        Member member = new Member();
        member.setEmail(DEMO_MEMBER_EMAIL);
        member.setPassword(passwordEncoder.encode("demo1234"));
        member.setName("陳小美");
        member.setPhone("0922333444");
        memberRepository.save(member);

        Address address = new Address();
        address.setMember(member);
        address.setRecipientName("陳小美");
        address.setPhone("0922333444");
        address.setPostalCode("110");
        address.setCity("台北市");
        address.setDistrict("信義區");
        address.setDetailAddress("松仁路100號");
        address.setDefault(true);
        addressRepository.save(address);

        record Item(String skuCode, int quantity) {
        }
        record Spec(int daysAgo, OrderStatus status, PaymentMethod paymentMethod, List<Item> items) {
        }

        List<Spec> specs = List.of(
                new Spec(20, OrderStatus.COMPLETED, PaymentMethod.CREDIT_CARD,
                        List.of(new Item("TSHIRT-BLK-M", 1), new Item("EARBUD-BLK", 1))),
                new Spec(17, OrderStatus.COMPLETED, PaymentMethod.ATM,
                        List.of(new Item("DRESS-FLR-S", 1))),
                new Spec(14, OrderStatus.COMPLETED, PaymentMethod.CREDIT_CARD,
                        List.of(new Item("CARD-BEG-M", 2))),
                new Spec(11, OrderStatus.CANCELLED, PaymentMethod.COD,
                        List.of(new Item("POLO-NVY-L", 1))),
                new Spec(8, OrderStatus.COMPLETED, PaymentMethod.CREDIT_CARD,
                        List.of(new Item("BOTTLE-BLK", 2), new Item("STORAGEBOX-3PK", 1))),
                new Spec(5, OrderStatus.PAID, PaymentMethod.ATM,
                        List.of(new Item("EARBUD-BLK", 1))),
                new Spec(2, OrderStatus.SHIPPING, PaymentMethod.CREDIT_CARD,
                        List.of(new Item("TSHIRT-BLK-L", 1), new Item("DRESS-FLR-M", 1))),
                new Spec(0, OrderStatus.PENDING_PAYMENT, PaymentMethod.COD,
                        List.of(new Item("BOTTLE-WHT", 1)))
        );

        int sequence = 1;
        for (Spec spec : specs) {
            Orders order = new Orders();
            order.setOrderNo("DEMO" + String.format("%08d", sequence++));
            order.setMember(member);
            order.setAddress(address);
            order.setPaymentMethod(spec.paymentMethod());
            order.setStatus(spec.status());
            order.setReceiverName(address.getRecipientName());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverAddress(address.getCity() + address.getDistrict() + address.getDetailAddress());

            BigDecimal total = BigDecimal.ZERO;
            for (Item item : spec.items()) {
                ProductSku sku = productSkuRepository.findBySkuCode(item.skuCode())
                        .orElseThrow(() -> new IllegalStateException("找不到示範用 SKU:" + item.skuCode()));

                BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
                total = total.add(subtotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setProductSku(sku);
                orderItem.setProductName(sku.getProduct().getName());
                orderItem.setSpecName(sku.getSpecName());
                orderItem.setUnitPrice(sku.getPrice());
                orderItem.setQuantity(item.quantity());
                orderItem.setSubtotal(subtotal);
                order.addItem(orderItem);
            }
            order.setSubtotalAmount(total);
            order.setTotalAmount(total);

            Orders saved = orderRepository.save(order);

            LocalDateTime backdatedAt = LocalDateTime.now().minusDays(spec.daysAgo());
            jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?", backdatedAt, saved.getId());
        }
    }
}
