package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.DeleteAccountRequest;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.repository.RefreshTokenRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 會員刪除帳號。帳號本身匿名化而不是真的刪列:訂單、評論等交易紀錄仍引用它,且帳務需要保留。
 */
@Service
@Transactional
public class AccountDeletionService {

    static final String DELETED_NAME = "已刪除會員";

    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public AccountDeletionService(MemberRepository memberRepository,
                                  AddressRepository addressRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  ApplicationEventPublisher eventPublisher) {
        this.memberRepository = memberRepository;
        this.addressRepository = addressRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    public void deleteAccount(Long memberId, DeleteAccountRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("會員不存在"));
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException("密碼不正確");
        }

        // 先讓各模組檢查(有處理中的訂單會丟例外)並清除自己的資料
        eventPublisher.publishEvent(new MemberDeletingEvent(memberId));

        member.setEmail("deleted-" + memberId + "@deleted.invalid");
        member.setName(DELETED_NAME);
        member.setPhone(null);
        member.setBirthday(null);
        member.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        member.setEmailVerified(false);
        member.setStatus(AccountStatus.DISABLED);

        // 地址可能被歷史訂單引用,不能刪;清除個資並保留列
        for (Address address : addressRepository.findByMemberIdOrderByIsDefaultDescIdDesc(memberId)) {
            address.setRecipientName(DELETED_NAME);
            address.setPhone("-");
            address.setPostalCode(null);
            address.setCity("-");
            address.setDistrict("-");
            address.setDetailAddress("-");
            address.setDefault(false);
        }
        refreshTokenRepository.revokeAllByMemberId(memberId);
    }
}
