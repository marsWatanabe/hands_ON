package com.aojt.mgmt.service;

import com.aojt.mgmt.dto.AuthenticationRequest;
import com.aojt.mgmt.dto.AuthenticationResponse;
import com.aojt.mgmt.dto.RegisterRequest;
import com.aojt.mgmt.dto.UserResponse;
import com.aojt.mgmt.entity.Role;
import com.aojt.mgmt.entity.User;
import com.aojt.mgmt.exception.ConflictException;
import com.aojt.mgmt.repository.UserRepository;
import com.aojt.mgmt.service.jwt.JwtService;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final String BAD_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = userRepository.save(new User(
                request.firstname(), request.lastname(), email,
                passwordEncoder.encode(request.password()), Role.USER));
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException(BAD_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse refresh(String refreshToken) {
        String email;
        try {
            email = jwtService.validateRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        return new AuthenticationResponse(
                jwtService.generateAccessToken(user), jwtService.generateRefreshToken(user), null);
    }

    private AuthenticationResponse tokens(User user) {
        return new AuthenticationResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
