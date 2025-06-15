package com.sellerscope.controller;

import com.sellerscope.dto.JwtAuthenticationResponse;
import com.sellerscope.dto.RefreshTokenRequest;
import com.sellerscope.dto.SignInRequest;
import com.sellerscope.dto.SignUpRequest;
import com.sellerscope.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationService authenticationService;

    @PostMapping("/sign-up")
    public ResponseEntity<JwtAuthenticationResponse> signUp(@RequestBody SignUpRequest request) {
        logger.info("Received sign-up request for email: {}", request.getEmail());
        return ResponseEntity.ok(authenticationService.signUp(request));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<JwtAuthenticationResponse> signIn(@RequestBody SignInRequest request) {
        logger.info("Received sign-in request for email: {}", request.getEmail());
        return ResponseEntity.ok(authenticationService.signIn(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) {
        logger.info("Received refresh token request");
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }
}