package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberStatusRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberService {

    MemberResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    MemberResponse getProfile(Long memberId);

    MemberResponse updateProfile(Long memberId, MemberUpdateRequest request);

    Page<MemberResponse> listAdmin(AccountStatus status, String keyword, Pageable pageable);

    MemberResponse getAdminDetail(Long memberId);

    MemberResponse updateStatus(Long memberId, MemberStatusRequest request);
}
