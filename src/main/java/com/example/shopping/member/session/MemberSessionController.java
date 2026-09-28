package com.example.shopping.member.session;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/me/sessions")
public class MemberSessionController {

    private final MemberSessionService sessionService;

    public MemberSessionController(MemberSessionService sessionService) {
        this.sessionService = sessionService;
    }

    public record RevokeOthersRequest(@NotBlank(message = "缺少目前裝置的工作階段") String currentSessionId) {
    }

    @GetMapping
    public ApiResponse<List<MemberSessionService.SessionResponse>> list() {
        return ApiResponse.success(sessionService.list(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> revoke(@PathVariable String sessionId) {
        sessionService.revoke(SecurityUtils.getCurrentUserId(), sessionId);
        return ApiResponse.success("已登出該裝置", null);
    }

    @PostMapping("/revoke-others")
    public ApiResponse<Integer> revokeOthers(@Valid @RequestBody RevokeOthersRequest request) {
        int count = sessionService.revokeOthers(SecurityUtils.getCurrentUserId(), request.currentSessionId());
        return ApiResponse.success("已登出其他 " + count + " 個裝置", count);
    }
}
