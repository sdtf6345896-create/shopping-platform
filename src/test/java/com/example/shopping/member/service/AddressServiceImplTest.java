package com.example.shopping.member.service;

import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.AddressRequest;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private AddressRequest requestWithDefault(boolean isDefault) {
        AddressRequest request = new AddressRequest();
        request.setRecipientName("王小明");
        request.setPhone("0912345678");
        request.setCity("台北市");
        request.setDistrict("大安區");
        request.setDetailAddress("復興南路一段1號");
        request.setDefaultAddress(isDefault);
        return request;
    }

    @Test
    void create_clearsExistingDefault_whenNewAddressIsDefault() {
        Address oldDefault = new Address();
        oldDefault.setId(1L);
        oldDefault.setDefault(true);

        when(addressRepository.findByMemberIdOrderByIsDefaultDescIdDesc(100L)).thenReturn(List.of(oldDefault));
        when(memberRepository.getReferenceById(100L)).thenReturn(new Member());
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.create(100L, requestWithDefault(true));

        assertThat(oldDefault.isDefault()).isFalse();
    }

    @Test
    void create_doesNotTouchOtherAddresses_whenNewAddressNotDefault() {
        when(memberRepository.getReferenceById(100L)).thenReturn(new Member());
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.create(100L, requestWithDefault(false));

        verify(addressRepository, never()).findByMemberIdOrderByIsDefaultDescIdDesc(any());
    }

    @Test
    void update_clearsPreviousDefault_whenSwitchingDefaultToAnotherAddress() {
        Address target = new Address();
        target.setId(2L);
        target.setDefault(false);

        Address oldDefault = new Address();
        oldDefault.setId(1L);
        oldDefault.setDefault(true);

        when(addressRepository.findByIdAndMemberId(2L, 100L)).thenReturn(Optional.of(target));
        when(addressRepository.findByMemberIdOrderByIsDefaultDescIdDesc(100L))
                .thenReturn(List.of(oldDefault, target));

        addressService.update(100L, 2L, requestWithDefault(true));

        assertThat(oldDefault.isDefault()).isFalse();
        assertThat(target.isDefault()).isTrue();
    }

    @Test
    void update_skipsClearStep_whenAddressAlreadyDefault() {
        Address target = new Address();
        target.setId(2L);
        target.setDefault(true);

        when(addressRepository.findByIdAndMemberId(2L, 100L)).thenReturn(Optional.of(target));

        addressService.update(100L, 2L, requestWithDefault(true));

        verify(addressRepository, never()).findByMemberIdOrderByIsDefaultDescIdDesc(any());
    }

    @Test
    void delete_throws_whenAddressNotOwnedByMember() {
        when(addressRepository.findByIdAndMemberId(2L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(100L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
