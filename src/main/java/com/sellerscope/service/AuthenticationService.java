package com.sellerscope.service;

import com.sellerscope.dto.JwtAuthenticationResponse;
import com.sellerscope.dto.RefreshTokenRequest;
import com.sellerscope.dto.SignInRequest;
import com.sellerscope.dto.SignUpRequest;
import com.sellerscope.entity.RefreshToken;
import com.sellerscope.entity.Role;
import com.sellerscope.entity.User;
import com.sellerscope.repository.RefreshTokenRepository;
import com.sellerscope.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public JwtAuthenticationResponse signUp(SignUpRequest request) {
        logger.info("Processing sign-up request for email: {}", request.getEmail());
        Optional<User> userExists = userRepository.findByEmail(request.getEmail());
        if (userExists.isPresent()) {
            logger.warn("User already exists with email: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        logger.info("User created with email: {}", request.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = saveRefreshToken(user);
        return JwtAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public JwtAuthenticationResponse signIn(SignInRequest request) {
        logger.info("Processing sign-in request for email: {}", request.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("Authentication successful for email: {}", request.getEmail());

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + request.getEmail()));

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = saveRefreshToken(user);
            return JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (org.springframework.security.core.AuthenticationException e) {
            logger.error("Authentication failed for email: {}. Error: {}", request.getEmail(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password", e);
        }
    }

    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Processing refresh token request");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            logger.warn("Refresh token expired for user: {}", refreshToken.getUser().getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = saveRefreshToken(user); // Ротация refresh token
        refreshTokenRepository.delete(refreshToken); // Удаляем старый refresh token
        logger.info("New tokens issued for user: {}", user.getEmail());

        return JwtAuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private String saveRefreshToken(User user) {
        String token = jwtService.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .expiryDate(jwtService.getRefreshTokenExpiry())
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token saved for user: {}", user.getEmail());
        return token;
    }

    @Transactional
    public void logout(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }
}