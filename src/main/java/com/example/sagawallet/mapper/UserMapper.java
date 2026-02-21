package com.example.sagawallet.mapper;

import com.example.sagawallet.dto.UserDto;
import com.example.sagawallet.entity.User;
import org.springframework.stereotype.Component;

public class UserMapper {
    public static User toEntity(UserDto dto) {
        return User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .build();
    }

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}

