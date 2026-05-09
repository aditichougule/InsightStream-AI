package com.aivideoip.service;

import com.aivideoip.dto.UserDTO;
import com.aivideoip.entity.User;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .active(user.getActive())
                .build();
    }

    public void updateUser(Long userId, UserDTO userDTO) {
        User user = getUserById(userId);
        
        if (userDTO.getFirstName() != null) {
            user.setFirstName(userDTO.getFirstName());
        }
        if (userDTO.getLastName() != null) {
            user.setLastName(userDTO.getLastName());
        }

        userRepository.save(user);
        log.info("User updated: {}", userId);
    }

    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
        log.info("User deleted: {}", userId);
    }
}
