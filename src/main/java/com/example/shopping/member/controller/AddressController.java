package com.example.shopping.member.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.member.dto.request.AddressRequest;
import com.example.shopping.member.dto.response.AddressResponse;
import com.example.shopping.member.service.AddressService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.success(addressService.list(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("新增成功", addressService.create(SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressResponse> update(@PathVariable Long addressId,
                                                @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("更新成功",
                addressService.update(SecurityUtils.getCurrentUserId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> delete(@PathVariable Long addressId) {
        addressService.delete(SecurityUtils.getCurrentUserId(), addressId);
        return ApiResponse.success("刪除成功", null);
    }
}
