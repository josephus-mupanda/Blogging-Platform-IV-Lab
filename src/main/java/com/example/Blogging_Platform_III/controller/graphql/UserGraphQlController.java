package com.example.Blogging_Platform_III.controller.graphql;

import com.example.Blogging_Platform_III.dto.UserCreate;
import com.example.Blogging_Platform_III.dto.UserUpdate;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.service.UserService;
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

    @QueryMapping
    public List<User> users() {
        return users.list();
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return users.get(id);
    }

    @MutationMapping
    public User createUser(@Argument @Valid UserCreate input) {
        return users.create(input.getUsername(), input.getEmail(), input.getDisplayName());
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument @Valid UserUpdate input) {
        return users.update(id, input.getEmail(), input.getDisplayName());
    }

    @MutationMapping
    public boolean deleteUser(@Argument Long id) {
        users.delete(id);
        return true;
    }
}
