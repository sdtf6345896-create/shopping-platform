package com.example.shopping.security;

import com.example.shopping.common.enums.Role;

public record AuthenticatedUser(Long id, String subject, Role role) {
}
