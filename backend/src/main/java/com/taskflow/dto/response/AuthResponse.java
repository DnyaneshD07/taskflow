package com.taskflow.dto.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, String username) {
    public static AuthResponse of(String token, long expiresIn, String username) {
        return new AuthResponse(token, "Bearer", expiresIn, username);
    }
}
