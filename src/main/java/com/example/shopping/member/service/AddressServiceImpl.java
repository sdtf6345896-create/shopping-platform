package com.example.shopping.member.service;

import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.AddressRequest;
import com.example.shopping.member.dto.response.AddressResponse;
import com.example.shopping.member.entity.Address;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.AddressRepository;
import com.example.shopping.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;

    public AddressServiceImpl(AddressRepository addressRepository, MemberRepository memberRepository) {
        this.addressRepository = addressRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long memberId) {
        return addressRepository.findByMemberIdOrderByIsDefaultDescIdDesc(memberId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Override
    public AddressResponse create(Long memberId, AddressRequest request) {
        Member member = memberRepository.getReferenceById(memberId);

        if (request.isDefaultAddress()) {
            clearExistingDefault(memberId);
        }

        Address address = new Address();
        address.setMember(member);
        applyRequest(address, request);

        return AddressResponse.from(addressRepository.save(address));
    }

    @Override
    public AddressResponse update(Long memberId, Long addressId, AddressRequest request) {
        Address address = findOwnedAddressOrThrow(memberId, addressId);

        if (request.isDefaultAddress() && !address.isDefault()) {
            clearExistingDefault(memberId);
        }

        applyRequest(address, request);
        return AddressResponse.from(address);
    }

    @Override
    public void delete(Long memberId, Long addressId) {
        Address address = findOwnedAddressOrThrow(memberId, addressId);
        addressRepository.delete(address);
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setPostalCode(request.getPostalCode());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setDefault(request.isDefaultAddress());
    }

    private void clearExistingDefault(Long memberId) {
        addressRepository.findByMemberIdOrderByIsDefaultDescIdDesc(memberId).stream()
                .filter(Address::isDefault)
                .forEach(a -> a.setDefault(false));
    }

    private Address findOwnedAddressOrThrow(Long memberId, Long addressId) {
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("地址不存在"));
    }
}
