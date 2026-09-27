package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.member.dto.request.ChangePasswordRequest;
import com.example.shopping.member.dto.request.ForgotPasswordRequest;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberStatusRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RefreshTokenRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.request.ResendVerificationRequest;
import com.example.shopping.member.dto.request.ResetPasswordRequest;
import com.example.shopping.member.dto.request.VerifyEmailRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberService {

    MemberResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void resendVerification(ResendVerificationRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    MemberResponse getProfile(Long memberId);

    MemberResponse updateProfile(Long memberId, MemberUpdateRequest request);

    /** 登入狀態下修改密碼:需驗證目前密碼,成功後撤銷所有 refresh token 並寄出安全通知 */
    void changePassword(Long memberId, ChangePasswordRequest request);

    Page<MemberResponse> listAdmin(AccountStatus status, String keyword, Pageable pageable);

    MemberResponse getAdminDetail(Long memberId);

    MemberResponse updateStatus(Long memberId, MemberStatusRequest request);
}
