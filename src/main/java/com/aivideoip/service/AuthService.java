package com.aivideoip.service;

import com.aivideoip.dto.UserDTO;
import com.aivideoip.dto.LoginRequest;
import com.aivideoip.dto.RegisterRequest;
import com.aivideoip.dto.AuthResponse;
import com.aivideoip.entity.User;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.UserRepository;
import com.aivideoip.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setRole(User.UserRole.USER);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with email: {}", savedUser.getEmail());

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());
        UserDTO userDTO = userService.convertToDTO(savedUser);

        return new AuthResponse(token, userDTO);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getEmail());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        UserDTO userDTO = userService.convertToDTO(user);

        return new AuthResponse(token, userDTO);
    }

    public UserDTO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return userService.convertToDTO(user);
    }
}
