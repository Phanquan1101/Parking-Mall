package com.parkflow.mall.identity.model;

import java.util.List;

public record DemoUser(
        String id,
        String username,
        String passwordHash,
        String displayName,
        List<Role> roles) {
}
