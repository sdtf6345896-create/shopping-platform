package com.example.shopping.member.session;

import jakarta.servlet.http.HttpServletRequest;

/** 登入 / 換發 token 時的用戶端資訊,用於「登入裝置管理」 */
public record ClientInfo(String userAgent, String ipAddress) {

    private static final int MAX_USER_AGENT = 255;
    private static final int MAX_IP = 45;

    public static ClientInfo unknown() {
        return new ClientInfo(null, null);
    }

    /** 經過 nginx 時取 X-Forwarded-For 的第一個位址(原始用戶端),否則取連線位址 */
    public static ClientInfo from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return new ClientInfo(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT), truncate(ip, MAX_IP));
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
