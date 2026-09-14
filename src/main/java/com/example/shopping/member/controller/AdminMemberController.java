package com.example.shopping.member.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.member.dto.request.MemberStatusRequest;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MemberResponse>> list(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ApiResponse.success(PageResponse.from(memberService.listAdmin(status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(memberService.getAdminDetail(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<MemberResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody MemberStatusRequest request) {
        return ApiResponse.success("狀態更新成功", memberService.updateStatus(id, request));
    }
}
