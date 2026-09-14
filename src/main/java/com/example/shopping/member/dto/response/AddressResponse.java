package com.example.shopping.member.dto.response;

import com.example.shopping.member.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String recipientName;
    private String phone;
    private String postalCode;
    private String city;
    private String district;
    private String detailAddress;
    private boolean defaultAddress;

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getPostalCode(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.isDefault());
    }
}
