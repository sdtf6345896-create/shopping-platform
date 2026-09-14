package com.example.shopping.security;

import com.example.shopping.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new BusinessException("尚未登入", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().id();
    }
}
