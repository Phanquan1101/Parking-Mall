package com.parkflow.mall.identity.service;

import com.parkflow.mall.identity.model.DemoUser;
import com.parkflow.mall.identity.model.Role;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DemoUserStore implements UserDetailsService {
    private final Map<String, DemoUser> users;

    public DemoUserStore(PasswordEncoder passwordEncoder) {
        users = Map.of(
                "admin", user("usr_admin", "admin", "admin123", "System Admin", List.of(Role.ADMIN), passwordEncoder),
                "staff", user("usr_staff", "staff", "staff123", "Parking Staff", List.of(Role.PARKING_STAFF), passwordEncoder),
                "merchant", user("usr_merchant", "merchant", "merchant123", "Merchant Staff", List.of(Role.MERCHANT_STAFF), passwordEncoder));
    }

    public Optional<DemoUser> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username.toLowerCase(Locale.ROOT)));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        DemoUser user = findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Demo user not found"));
        return User.withUsername(user.username())
                .password(user.passwordHash())
                .authorities(user.roles().stream().map(role -> "ROLE_" + role.name()).toArray(String[]::new))
                .build();
    }

    private DemoUser user(String id, String username, String password, String displayName, List<Role> roles, PasswordEncoder passwordEncoder) {
        return new DemoUser(id, username, passwordEncoder.encode(password), displayName, roles);
    }
}
