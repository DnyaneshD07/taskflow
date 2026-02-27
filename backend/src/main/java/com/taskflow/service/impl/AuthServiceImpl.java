package com.taskflow.service.impl;

import com.taskflow.domain.entity.User;
import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.exception.ConflictException;
import com.taskflow.repository.RoleRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider      jwtProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new ConflictException("Username already taken");
        if (userRepository.existsByEmail(req.email()))
            throw new ConflictException("Email already registered");

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));

        roleRepository.findByName("ROLE_USER").ifPresent(user.getRoles()::add);
        userRepository.save(user);

        log.info("Registered new user: {}", req.username());
        return login(new LoginRequest(req.username(), req.password()));
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        String token = jwtProvider.generate(auth);
        return AuthResponse.of(token, jwtProvider.getExpirationMs(), req.username());
    }
}
