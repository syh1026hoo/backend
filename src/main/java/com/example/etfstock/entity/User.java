package com.example.etfstock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * 전략과제 #2: 사용자별 관심종목 기능을 위한 사용자 정보
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자명 (고유)
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 이메일 (고유)
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 사용자 실명
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * 패스워드 (해시된 값)
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * 사용자 상태 (ACTIVE, INACTIVE, SUSPENDED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 계정 생성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 정보 수정일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 마지막 로그인 일시
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 관심종목 목록 (지연 로딩, JSON 직렬화 제외)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<WatchList> watchLists = new ArrayList<>();

    /**
     * 알림 조건 목록 (지연 로딩, JSON 직렬화 제외)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<AlertCondition> alertConditions = new ArrayList<>();

    /**
     * 알림 이력 목록 (지연 로딩, JSON 직렬화 제외)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Alert> alerts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 사용자 상태 열거형
     */
    public enum UserStatus {
        ACTIVE("활성"),
        INACTIVE("비활성"),
        SUSPENDED("정지");

        private final String description;

        UserStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 관심종목 추가
     */
    public void addWatchList(WatchList watchList) {
        watchLists.add(watchList);
        watchList.setUser(this);
    }

    /**
     * 관심종목 제거
     */
    public void removeWatchList(WatchList watchList) {
        watchLists.remove(watchList);
        watchList.setUser(null);
    }

    /**
     * 특정 ETF가 관심종목에 포함되어 있는지 확인
     */
    public boolean hasWatchListItem(String isinCd) {
        return watchLists.stream()
                .anyMatch(w -> w.getIsinCd().equals(isinCd) && w.isActive());
    }

    /**
     * 활성 관심종목 개수
     */
    public long getActiveWatchListCount() {
        return watchLists.stream()
                .filter(WatchList::isActive)
                .count();
    }

    /**
     * 알림 조건 추가
     */
    public void addAlertCondition(AlertCondition alertCondition) {
        alertConditions.add(alertCondition);
        alertCondition.setUser(this);
    }

    /**
     * 알림 조건 제거
     */
    public void removeAlertCondition(AlertCondition alertCondition) {
        alertConditions.remove(alertCondition);
        alertCondition.setUser(null);
    }

    /**
     * 활성 알림 조건 개수
     */
    public long getActiveAlertConditionCount() {
        return alertConditions.stream()
                .filter(AlertCondition::isActive)
                .count();
    }

    /**
     * 알림 추가
     */
    public void addAlert(Alert alert) {
        alerts.add(alert);
        alert.setUser(this);
    }

    /**
     * 읽지 않은 알림 개수
     */
    public long getUnreadAlertCount() {
        return alerts.stream()
                .filter(alert -> !alert.isRead())
                .count();
    }

    /**
     * 최근 알림 개수 (24시간 이내)
     */
    public long getRecentAlertCount() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        return alerts.stream()
                .filter(alert -> alert.getTriggeredAt().isAfter(yesterday))
                .count();
    }
}
