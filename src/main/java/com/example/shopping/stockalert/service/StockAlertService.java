package com.example.shopping.stockalert.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.mail.MailSender;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import com.example.shopping.stockalert.entity.StockAlert;
import com.example.shopping.stockalert.repository.StockAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final ProductSkuRepository productSkuRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final MailSender mailSender;

    public StockAlertService(StockAlertRepository stockAlertRepository,
                             ProductSkuRepository productSkuRepository,
                             MemberRepository memberRepository,
                             NotificationService notificationService,
                             MailSender mailSender) {
        this.stockAlertRepository = stockAlertRepository;
        this.productSkuRepository = productSkuRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.mailSender = mailSender;
    }

    /** 訂閱貨到通知;只有缺貨中的規格可以訂閱,重複訂閱不會重複建立 */
    public void subscribe(Long memberId, Long skuId) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("商品規格不存在"));
        if (sku.getStock() > 0) {
            throw new BusinessException("此規格目前有庫存,可以直接購買");
        }
        if (stockAlertRepository.findByMemberIdAndProductSkuId(memberId, skuId).isPresent()) {
            return;
        }
        StockAlert alert = new StockAlert();
        alert.setMember(memberRepository.getReferenceById(memberId));
        alert.setProductSku(sku);
        stockAlertRepository.save(alert);
    }

    public void unsubscribe(Long memberId, Long skuId) {
        stockAlertRepository.findByMemberIdAndProductSkuId(memberId, skuId).ifPresent(stockAlertRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<Long> subscribedSkuIds(Long memberId, Long productId) {
        return stockAlertRepository.findSubscribedSkuIds(memberId, productId);
    }

    /**
     * 通知所有已補貨的訂閱者(站內通知 + email),通知後刪除訂閱。
     *
     * @return 通知筆數
     */
    public int notifyRestocked() {
        List<StockAlert> restocked = stockAlertRepository.findRestocked(ProductStatus.ON_SALE);
        for (StockAlert alert : restocked) {
            ProductSku sku = alert.getProductSku();
            Product product = sku.getProduct();
            String label = "「" + product.getName() + " " + sku.getSpecName() + "」";
            notificationService.notify(alert.getMember().getId(), NotificationType.SYSTEM, "貨到通知",
                    label + "已補貨,數量有限,欲購從速!", "/products/" + product.getId());
            mailSender.send(alert.getMember().getEmail(), "貨到通知:" + product.getName(),
                    "您好,\n\n您關注的" + label + "已經補貨,歡迎回來選購。");
        }
        stockAlertRepository.deleteAll(restocked);
        return restocked.size();
    }
}
