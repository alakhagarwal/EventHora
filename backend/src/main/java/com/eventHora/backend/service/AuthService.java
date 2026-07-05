package com.eventHora.backend.service;

import com.eventHora.backend.dto.*;
import com.eventHora.backend.exception.DuplicateResourceException;
import com.eventHora.backend.exception.InvalidCredentialsException;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.security.JwtProvider;
import com.eventHora.backend.model.SystemUser;
import com.eventHora.backend.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SystemUserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticate a system user (ADMIN or STAFF) and return a JWT.
     */
    public LoginResponse login(LoginRequest request) {
        try {
            // Delegates to DaoAuthenticationProvider → BCrypt password check
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        SystemUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtProvider.generateToken(user);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /**
     * ADMIN creates a new STAFF or ADMIN account.
     */
    public UserProfileResponse createUser(CreateStaffRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        SystemUser newUser = SystemUser.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        SystemUser saved = userRepository.save(newUser);

        return toProfileResponse(saved);
    }

    /**
     * Return the profile of the currently authenticated user.
     */
    public UserProfileResponse getProfile(String email) {
        SystemUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toProfileResponse(user);
    }

    /**
     * ADMIN lists all system users.
     */
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    /**
     * ADMIN deactivates a user account.
     */
    public void deactivateUser(String email) {
        SystemUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        user.setActive(false);
        userRepository.save(user);
    }

    // --- Private helpers ---

    private UserProfileResponse toProfileResponse(SystemUser user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
