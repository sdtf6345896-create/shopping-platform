package com.example.shopping.security;

import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AdminRepository adminRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminRepository = adminRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.isValid(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            AuthenticatedUser user = jwtTokenProvider.toAuthenticatedUser(claims);
            Role role = user.role();
            if (role.isBackOffice()) {
                // 後台帳號每次都以資料庫為準:停用立即失效,角色調整立即生效(不必等 token 過期)
                role = adminRepository.findActiveRoleById(user.id()).map(Role::valueOf).orElse(null);
                if (role == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                user = new AuthenticatedUser(user.id(), user.subject(), role);
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.HEADER_NAME);
        if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
