package com.sellerscope.controller;

import com.sellerscope.dto.JwtAuthenticationResponse;
import com.sellerscope.dto.RefreshTokenRequest;
import com.sellerscope.dto.SignInRequest;
import com.sellerscope.dto.SignUpRequest;
import com.sellerscope.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        logger.info("Received logout request");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            logger.warn("No valid authentication found for logout request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        logger.info("Logging out user: {}", userDetails.getUsername());
        authenticationService.logout(userDetails);
        return ResponseEntity.ok().build();
    }
}