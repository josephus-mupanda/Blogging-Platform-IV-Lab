package com.example.Blogging_Platform_IV.security;

import com.example.Blogging_Platform_IV.entity.Role;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import com.example.Blogging_Platform_IV.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Registration can only create AUTHOR and READER accounts, so the first ADMIN has to come from
 * somewhere else: this class creates it at startup from the app.admin.* properties.
 * It does nothing when that account already exists.
 */
@Component
public class AdminSeeder implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserService userService;
    private final UserRepository users;
    private final String username;
    private final String email;
    private final String password;

    public AdminSeeder(UserService userService, UserRepository users,
                       @Value("${app.admin.username}") String username,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.password}") String password) {
        this.userService = userService;
        this.users = users;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByUsername(username) || users.existsByEmail(email)) {
            return;
        }
        userService.create(username, email, "Administrator", password, Role.ADMIN);
        LOG.info("Created the ADMIN account '{}'", username);
    }
}
