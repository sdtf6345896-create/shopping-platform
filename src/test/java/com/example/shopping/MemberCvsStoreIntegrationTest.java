package com.example.shopping;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.cvs.MemberCvsStoreService;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.shipping.CvsBrand;
import com.example.shopping.order.shipping.CvsPickupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 常用門市:重複儲存不新增、上限 5 筆、只能刪自己的、刪除帳號時清空 */
@SpringBootTest
@ActiveProfiles("test")
class MemberCvsStoreIntegrationTest {

    @Autowired
    private MemberCvsStoreService storeService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private Member member(String name) {
        Member member = new Member();
        member.setEmail(name + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        member.setPassword("x");
        member.setName(name);
        return memberRepository.save(member);
    }

    private static CvsPickupRequest store(String storeName, String code) {
        CvsPickupRequest request = new CvsPickupRequest();
        request.setBrand(CvsBrand.SEVEN_ELEVEN);
        request.setStoreName(storeName);
        request.setStoreCode(code);
        request.setRecipientName("王小明");
        request.setRecipientPhone("0912345678");
        return request;
    }

    @Test
    void savedStoresLifecycle() {
        Member owner = member("owner");
        Member other = member("other");

        MemberCvsStoreService.StoreResponse first = storeService.save(owner.getId(), store(" 信義門市 ", ""));
        // 同一門市 + 取件人再存一次:不新增,只更新店號
        storeService.save(owner.getId(), store("信義門市", "123456"));
        assertThat(storeService.list(owner.getId())).singleElement().satisfies(s -> {
            assertThat(s.id()).isEqualTo(first.id());
            assertThat(s.storeCode()).isEqualTo("123456");
            assertThat(s.brandLabel()).isEqualTo("7-ELEVEN");
        });

        for (int i = 2; i <= 5; i++) {
            storeService.save(owner.getId(), store("門市" + i, null));
        }
        assertThatThrownBy(() -> storeService.save(owner.getId(), store("第六間", null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 5 筆");

        assertThatThrownBy(() -> storeService.delete(other.getId(), first.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        storeService.delete(owner.getId(), first.id());
        assertThat(storeService.list(owner.getId())).hasSize(4);

        transactionTemplate.executeWithoutResult(s -> eventPublisher.publishEvent(new MemberDeletingEvent(owner.getId())));
        assertThat(storeService.list(owner.getId())).isEmpty();
    }
}
