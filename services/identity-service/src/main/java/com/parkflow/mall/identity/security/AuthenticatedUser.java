package com.parkflow.mall.identity.security;

import com.parkflow.mall.identity.model.Role;
import java.util.List;

public record AuthenticatedUser(
        String id, String username, String displayName, List<Role> roles) {
}
