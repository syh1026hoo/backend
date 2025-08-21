package com.example.etfstock.dto;

import com.example.etfstock.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보 DTO
 * 전략과제 #2: 웹 인터페이스용 사용자 데이터 전송 객체
 */
@Data
@NoArgsConstructor
public class UserDto {
    
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private long activeWatchListCount;

    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.status = user.getStatus().name();
        this.createdAt = user.getCreatedAt();
        this.lastLoginAt = user.getLastLoginAt();
        this.activeWatchListCount = user.getActiveWatchListCount();
    }
}
