package com.example.Blogging_Platform_III.controller.rest;

import static com.example.Blogging_Platform_III.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_III.dto.UserCreate;
import com.example.Blogging_Platform_III.dto.UserUpdate;
import com.example.Blogging_Platform_III.model.UserResponse;
import com.example.Blogging_Platform_III.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    ApiResponse<List<UserResponse>> list() {
        return ok("Users found.", users.list());
    }

    @GetMapping("/{id}")
    ApiResponse<UserResponse> get(@PathVariable long id) {
        return ok("User found.", users.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<UserResponse> create(@Valid @RequestBody UserCreate input) {
        return ok("User created.", users.create(input.getUsername(), input.getEmail(), input.getDisplayName()));
    }

    @PutMapping("/{id}")
    ApiResponse<UserResponse> update(@PathVariable long id, @Valid @RequestBody UserUpdate input) {
        return ok("User updated.", users.update(id, input.getEmail(), input.getDisplayName()));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable long id) {
        users.delete(id);
        return ok("User deleted.", null);
    }
}
