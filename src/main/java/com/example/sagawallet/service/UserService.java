package com.example.sagawallet.service;

import com.example.sagawallet.dto.UserDto;
import com.example.sagawallet.entity.User;
import com.example.sagawallet.mapper.UserMapper;
import com.example.sagawallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDto createUser(UserDto request) {
        User user = UserMapper.toEntity(request);
        User saved = userRepository.save(user);
        return UserMapper.toDto(saved);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserMapper.toDto(user);
    }

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserMapper.toDto(user);
    }
}
