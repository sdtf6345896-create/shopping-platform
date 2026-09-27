package com.example.shopping.returns.event;

import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.returns.repository.ReturnRequestRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 退貨審核中的會員不能刪除帳號(退款需要聯絡得到本人) */
@Component
public class ReturnMemberGuard {

    private final ReturnRequestRepository returnRequestRepository;

    public ReturnMemberGuard(ReturnRequestRepository returnRequestRepository) {
        this.returnRequestRepository = returnRequestRepository;
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        if (returnRequestRepository.existsByOrderMemberIdAndStatus(event.memberId(), ReturnStatus.PENDING)) {
            throw new BusinessException("尚有審核中的退貨申請,請等處理完成後再刪除帳號");
        }
    }
}
