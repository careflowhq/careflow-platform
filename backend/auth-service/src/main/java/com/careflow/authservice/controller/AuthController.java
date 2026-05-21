package com.careflow.authservice.controller;

import com.careflow.authservice.dto.InviteRequest;
import com.careflow.authservice.dto.InviteResponse;
import com.careflow.authservice.dto.LoginRequest;
import com.careflow.authservice.dto.LoginResponse;
import com.careflow.authservice.dto.RegisterInviteRequest;
import com.careflow.authservice.dto.RegisterRequest;
import com.careflow.authservice.service.AuthService;
import com.careflow.authservice.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InviteService inviteService;

    @PostMapping({"/register", "/register-clinic"})
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/register-invite")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerInvite(@Valid @RequestBody RegisterInviteRequest request) {
        inviteService.acceptInvite(request);
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse invite(@Valid @RequestBody InviteRequest request) {
        return inviteService.createInvite(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}