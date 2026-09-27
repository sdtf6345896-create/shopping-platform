package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.dto.request.DeleteAccountRequest;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountDeletionService accountDeletionService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(1L);
        member.setEmail("me@example.com");
        member.setName("王小明");
        member.setPhone("0912345678");
        member.setPassword("encoded");
        member.setEmailVerified(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    private DeleteAccountRequest request(String password) {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword(password);
        return request;
    }

    @Test
    void deleteAccount_anonymizesMemberAndAddresses_revokesSessions() {
        Address address = new Address();
        address.setRecipientName("王小明");
        address.setPhone("0912345678");
        address.setDetailAddress("復興南路一段1號");
        address.setDefault(true);
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("random");
        when(addressRepository.findByMemberIdOrderByIsDefaultDescIdDesc(1L)).thenReturn(List.of(address));

        accountDeletionService.deleteAccount(1L, request("secret"));

        verify(eventPublisher).publishEvent(new MemberDeletingEvent(1L));
        assertThat(member.getEmail()).isEqualTo("deleted-1@deleted.invalid");
        assertThat(member.getName()).isEqualTo("已刪除會員");
        assertThat(member.getPhone()).isNull();
        assertThat(member.getStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(member.isEmailVerified()).isFalse();
        assertThat(address.getDetailAddress()).isEqualTo("-");
        assertThat(address.getPhone()).isEqualTo("-");
        assertThat(address.isDefault()).isFalse();
        verify(refreshTokenRepository).revokeAllByMemberId(1L);
    }

    @Test
    void deleteAccount_throws_whenPasswordWrong() {
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> accountDeletionService.deleteAccount(1L, request("wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密碼不正確");
        verify(eventPublisher, never()).publishEvent(any());
        assertThat(member.getEmail()).isEqualTo("me@example.com");
    }

    @Test
    void deleteAccount_keepsAccount_whenAModuleVetoes() {
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        doThrow(new BusinessException("尚有處理中的訂單")).when(eventPublisher).publishEvent(any(MemberDeletingEvent.class));

        assertThatThrownBy(() -> accountDeletionService.deleteAccount(1L, request("secret")))
                .hasMessageContaining("處理中的訂單");
        assertThat(member.getEmail()).isEqualTo("me@example.com");
        verify(refreshTokenRepository, never()).revokeAllByMemberId(any());
    }
}
