package com.example.readys7project.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String role;
    private String phoneNumber;
    private String location;
    private String avatarUrl;
    private String description;
    private LocalDateTime createdAt;
}
