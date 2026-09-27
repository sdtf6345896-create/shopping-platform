package com.example.shopping.stockalert.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.mail.MailSender;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import com.example.shopping.stockalert.entity.StockAlert;
import com.example.shopping.stockalert.repository.StockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private StockAlertRepository stockAlertRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MailSender mailSender;

    @InjectMocks
    private StockAlertService stockAlertService;

    private ProductSku sku;
    private Member member;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setId(10L);
        product.setName("無線耳機");
        product.setStatus(ProductStatus.ON_SALE);
        sku = new ProductSku();
        sku.setId(3L);
        sku.setProduct(product);
        sku.setSpecName("黑色");
        sku.setStock(0);
        member = new Member();
        member.setId(1L);
        member.setEmail("member@example.com");
    }

    @Test
    void subscribe_savesAlert_forSoldOutSku() {
        when(productSkuRepository.findById(3L)).thenReturn(Optional.of(sku));
        when(stockAlertRepository.findByMemberIdAndProductSkuId(1L, 3L)).thenReturn(Optional.empty());
        when(memberRepository.getReferenceById(1L)).thenReturn(member);

        stockAlertService.subscribe(1L, 3L);

        verify(stockAlertRepository).save(any(StockAlert.class));
    }

    @Test
    void subscribe_isIdempotent() {
        when(productSkuRepository.findById(3L)).thenReturn(Optional.of(sku));
        when(stockAlertRepository.findByMemberIdAndProductSkuId(1L, 3L)).thenReturn(Optional.of(new StockAlert()));

        stockAlertService.subscribe(1L, 3L);

        verify(stockAlertRepository, never()).save(any());
    }

    @Test
    void subscribe_rejectsSkuThatIsInStock() {
        sku.setStock(2);
        when(productSkuRepository.findById(3L)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> stockAlertService.subscribe(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("有庫存");
    }

    @Test
    void notifyRestocked_notifiesInAppAndByEmail_thenRemovesAlerts() {
        sku.setStock(5);
        StockAlert alert = new StockAlert();
        alert.setMember(member);
        alert.setProductSku(sku);
        when(stockAlertRepository.findRestocked(ProductStatus.ON_SALE)).thenReturn(List.of(alert));

        int notified = stockAlertService.notifyRestocked();

        assertThat(notified).isEqualTo(1);
        verify(notificationService).notify(eq(1L), eq(NotificationType.SYSTEM), eq("貨到通知"),
                contains("無線耳機 黑色"), eq("/products/10"));
        verify(mailSender).send(eq("member@example.com"), contains("無線耳機"), anyString());
        verify(stockAlertRepository).deleteAll(List.of(alert));
    }
}
