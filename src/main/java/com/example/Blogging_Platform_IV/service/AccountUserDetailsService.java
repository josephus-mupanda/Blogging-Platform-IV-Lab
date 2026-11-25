package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to find an account: it loads the user from PostgreSQL by username and
 * gives Spring Security the BCrypt hash and the role, so the framework can check the password.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public AccountUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account named " + username));
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }
}
