package com.example.shopping.member.service;

import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;

public interface MemberService {

    MemberResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    MemberResponse getProfile(Long memberId);

    MemberResponse updateProfile(Long memberId, MemberUpdateRequest request);
}
