package com.example.Blogging_Platform_IV.controller.graphql;

import org.springframework.security.access.prepost.PreAuthorize;
import com.example.Blogging_Platform_IV.dto.UserCreate;
import com.example.Blogging_Platform_IV.dto.UserUpdate;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class UserGraphQlController {
    private final UserService users;

    public UserGraphQlController(UserService users) {
        this.users = users;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @QueryMapping
    public List<User> users() {
        return users.list();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @QueryMapping
    public User user(@Argument Long id) {
        return users.get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public User createUser(@Argument @Valid UserCreate input) {
        return users.create(input.getUsername(), input.getEmail(), input.getDisplayName(),
                input.getPassword(), input.getRole());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public User updateUser(@Argument Long id, @Argument @Valid UserUpdate input) {
        return users.update(id, input.getEmail(), input.getDisplayName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public boolean deleteUser(@Argument Long id) {
        users.delete(id);
        return true;
    }
}
