package com.parkflow.mall.identity.service;

import com.parkflow.mall.identity.dto.LoginRequest;
import com.parkflow.mall.identity.dto.LoginResponse;
import com.parkflow.mall.identity.dto.UserResponse;
import com.parkflow.mall.identity.model.DemoUser;
import com.parkflow.mall.identity.security.AuthenticatedUser;
import com.parkflow.mall.identity.security.JwtService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService {
    private final DemoUserStore demoUserStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(DemoUserStore demoUserStore, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.demoUserStore = demoUserStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        DemoUser user = demoUserStore.findByUsername(request.username())
                .filter(candidate -> request.password() != null && passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return new LoginResponse(jwtService.issue(user), "Bearer", jwtService.expirationSeconds(), toResponse(user));
    }

    public UserResponse toResponse(AuthenticatedUser user) {
        return new UserResponse(user.id(), user.username(), user.displayName(), roleNames(user.roles().stream().map(Enum::name).toList()));
    }

    private UserResponse toResponse(DemoUser user) {
        return new UserResponse(user.id(), user.username(), user.displayName(), roleNames(user.roles().stream().map(Enum::name).toList()));
    }

    private List<String> roleNames(List<String> roles) {
        return roles;
    }
}
