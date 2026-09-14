package com.example.shopping.member.service;

import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberServiceImpl(MemberRepository memberRepository,
                              PasswordEncoder passwordEncoder,
                              JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public MemberResponse register(RegisterRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("此 Email 已被註冊");
        }

        Member member = new Member();
        member.setEmail(request.getEmail());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setName(request.getName());
        member.setPhone(request.getPhone());

        return MemberResponse.from(memberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException("帳號或密碼錯誤");
        }

        String token = jwtTokenProvider.generateToken(member.getId(), member.getEmail(), Role.MEMBER);
        return LoginResponse.of(token, member.getId(), member.getName(), member.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getProfile(Long memberId) {
        return MemberResponse.from(findMemberOrThrow(memberId));
    }

    @Override
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        Member member = findMemberOrThrow(memberId);
        member.setName(request.getName());
        member.setPhone(request.getPhone());
        return MemberResponse.from(member);
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("會員不存在"));
    }
}
