package com.careflow.authservice.controller;

import com.careflow.authservice.dto.LoginRequest;
import com.careflow.authservice.dto.LoginResponse;
import com.careflow.authservice.dto.RegisterRequest;
import com.careflow.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}