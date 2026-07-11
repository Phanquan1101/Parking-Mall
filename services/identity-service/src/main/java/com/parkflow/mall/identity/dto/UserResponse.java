package com.parkflow.mall.identity.dto;

import java.util.List;

public record UserResponse(String id, String username, String displayName, List<String> roles) {
}
