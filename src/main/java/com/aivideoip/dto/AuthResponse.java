package com.aivideoip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserDTO user;

    public AuthResponse(String accessToken, UserDTO user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = 86400L; // 24 hours
        this.user = user;
    }
}
