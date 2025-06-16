package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.dto.request.LoginRequest;
import com.workflow.cmsflowable.dto.request.UserCreateRequest;
import com.workflow.cmsflowable.dto.response.LoginResponse;
import com.workflow.cmsflowable.entity.User;
import com.workflow.cmsflowable.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login and token management")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate user and return JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Logout user and invalidate token")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // Extract token from "Bearer " prefix
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        authService.logout(jwtToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate Token", description = "Validate JWT token")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (authService.validateToken(jwtToken)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    @PostMapping("/register")
    @Operation(summary = "Create User", description = "Create a new user account")
    public ResponseEntity<String> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) {
        User user = authService.createUser(userCreateRequest);
        return ResponseEntity.ok("User created successfully with ID: " + user.getUserId());
    }
}