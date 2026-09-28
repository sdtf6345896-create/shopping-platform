package com.example.shopping.member.cvs;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.order.shipping.CvsBrand;
import com.example.shopping.order.shipping.CvsPickupRequest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 常用超商門市:每位會員最多 MAX_STORES 筆;同一門市與取件人重複儲存時不新增 */
@Service
@Transactional
public class MemberCvsStoreService {

    static final int MAX_STORES = 5;

    private final MemberCvsStoreRepository storeRepository;

    public MemberCvsStoreService(MemberCvsStoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public record StoreResponse(Long id, CvsBrand brand, String brandLabel, String storeName, String storeCode,
                                String recipientName, String recipientPhone) {

        static StoreResponse from(MemberCvsStore store) {
            return new StoreResponse(store.getId(), store.getBrand(), store.getBrand().getLabel(),
                    store.getStoreName(), store.getStoreCode(), store.getRecipientName(), store.getRecipientPhone());
        }
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> list(Long memberId) {
        return storeRepository.findByMemberIdOrderByIdDesc(memberId).stream().map(StoreResponse::from).toList();
    }

    public StoreResponse save(Long memberId, CvsPickupRequest request) {
        String storeName = request.getStoreName().trim();
        String storeCode = request.getStoreCode() == null || request.getStoreCode().isBlank()
                ? null : request.getStoreCode().trim();
        String recipientName = request.getRecipientName().trim();
        String recipientPhone = request.getRecipientPhone().trim();

        List<MemberCvsStore> existing = storeRepository.findByMemberIdOrderByIdDesc(memberId);
        for (MemberCvsStore store : existing) {
            if (store.getBrand() == request.getBrand() && store.getStoreName().equals(storeName)
                    && store.getRecipientName().equals(recipientName)
                    && store.getRecipientPhone().equals(recipientPhone)) {
                // 已存過同一個門市與取件人:只更新店號,不重複新增
                store.setStoreCode(storeCode);
                return StoreResponse.from(store);
            }
        }
        if (existing.size() >= MAX_STORES) {
            throw new BusinessException("常用門市最多 " + MAX_STORES + " 筆,請先刪除不常用的門市");
        }
        MemberCvsStore store = new MemberCvsStore();
        store.setMemberId(memberId);
        store.setBrand(request.getBrand());
        store.setStoreName(storeName);
        store.setStoreCode(storeCode);
        store.setRecipientName(recipientName);
        store.setRecipientPhone(recipientPhone);
        return StoreResponse.from(storeRepository.save(store));
    }

    public void delete(Long memberId, Long storeId) {
        MemberCvsStore store = storeRepository.findByIdAndMemberId(storeId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("門市不存在"));
        storeRepository.delete(store);
    }

    /** 刪除帳號時一併清掉常用門市(含取件人姓名與手機等個資) */
    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        storeRepository.deleteAllByMemberId(event.memberId());
    }
}
