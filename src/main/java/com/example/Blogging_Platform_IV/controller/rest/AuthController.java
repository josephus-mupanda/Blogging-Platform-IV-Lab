package com.example.Blogging_Platform_IV.controller.rest;

import static com.example.Blogging_Platform_IV.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_IV.dto.AuthToken;
import com.example.Blogging_Platform_IV.dto.LoginRequest;
import com.example.Blogging_Platform_IV.dto.UserCreate;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The public endpoints: no token is needed to register or to log in. */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Create an account (public)",
            description = "The role is READER or AUTHOR (READER when missing). An ADMIN cannot be created here.")
    public ApiResponse<User> register(@Valid @RequestBody UserCreate input) {
        return ok("Account created.", auth.register(input));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in and receive a JWT (public)",
            description = "Send the token in the header 'Authorization: Bearer <token>'. 401 when the credentials are wrong.")
    public ApiResponse<AuthToken> login(@Valid @RequestBody LoginRequest input) {
        return ok("Login successful.", auth.login(input.username(), input.password()));
    }
}
