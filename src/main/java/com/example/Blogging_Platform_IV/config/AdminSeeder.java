package com.example.Blogging_Platform_IV.config;

import com.example.Blogging_Platform_IV.enums.Role;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import com.example.Blogging_Platform_IV.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Registration cannot create an ADMIN, so the first one is created when the app starts
// (values in application.properties). Nothing happens if it already exists.
@Component
public class AdminSeeder implements ApplicationRunner {
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
        if (!users.existsByUsername(username) && !users.existsByEmail(email)) {
            userService.create(username, email, "Administrator", password, Role.ADMIN);
        }
    }
}
