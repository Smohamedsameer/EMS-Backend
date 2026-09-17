package com.ems.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper to pull the authenticated principal out of the security context inside controllers/services. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static CustomUserDetails get() {
        return (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static Long userId() {
        return get().getId();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(get().getRole());
    }
}
