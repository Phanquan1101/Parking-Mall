package com.parkflow.mall.identity.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
