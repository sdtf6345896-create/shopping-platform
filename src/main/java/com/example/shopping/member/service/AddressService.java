package com.example.shopping.member.service;

import com.example.shopping.member.dto.request.AddressRequest;
import com.example.shopping.member.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> list(Long memberId);

    AddressResponse create(Long memberId, AddressRequest request);

    AddressResponse update(Long memberId, Long addressId, AddressRequest request);

    void delete(Long memberId, Long addressId);
}
