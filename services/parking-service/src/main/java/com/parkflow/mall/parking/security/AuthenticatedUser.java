package com.parkflow.mall.parking.security;

import com.parkflow.mall.parking.model.Role;
import java.util.List;

public record AuthenticatedUser(String id, String username, String displayName, List<Role> roles) {
}
