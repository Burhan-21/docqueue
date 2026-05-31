package com.docqueue.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long   expiresIn;
    private final Long   userId;
    private final String name;
    private final String role;
    private final boolean otpRequired;
    private final String message;
}
