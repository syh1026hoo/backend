package com.example.etfstock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 관심종목 엔티티
 * 전략과제 #2: 사용자별 관심종목 관리
 */
@Entity
@Table(name = "user_watchlist", 
       indexes = {
           @Index(name = "idx_watchlist_user_id", columnList = "user_id"),
           @Index(name = "idx_watchlist_isin_cd", columnList = "isin_cd"),
           @Index(name = "idx_watchlist_user_etf", columnList = "user_id, isin_cd", unique = true),
           @Index(name = "idx_watchlist_active", columnList = "is_active"),
           @Index(name = "idx_watchlist_created", columnList = "created_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 정보 (JSON 직렬화 제외)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    /**
     * ETF 종목 코드 (ISIN 코드)
     */
    @Column(name = "isin_cd", nullable = false, length = 12)
    private String isinCd;

    /**
     * ETF 종목명 (캐시용 - 조회 성능 향상)
     */
    @Column(name = "etf_name", length = 200)
    private String etfName;

    /**
     * ETF 단축코드 (캐시용)
     */
    @Column(name = "short_code", length = 10)
    private String shortCode;

    /**
     * 활성 상태 (true: 관심종목, false: 관심해제)
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * 관심종목 추가일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 수정일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 관심종목 해제일시
     */
    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    /**
     * 메모/태그 (사용자가 추가할 수 있는 개인 메모)
     */
    @Column(name = "memo", length = 500)
    private String memo;

    /**
     * 알림 설정 (가격 변동 알림 등을 위한 향후 확장)
     */
    @Column(name = "notification_enabled")
    private boolean notificationEnabled = true;

    /**
     * 알림 조건 목록 (지연 로딩, JSON 직렬화 제외)
     */
    @OneToMany(mappedBy = "watchList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<AlertCondition> alertConditions = new ArrayList<>();

    /**
     * 알림 이력 목록 (지연 로딩, JSON 직렬화 제외)
     */
    @OneToMany(mappedBy = "watchList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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
     * 관심종목 활성화
     */
    public void activate() {
        this.active = true;
        this.removedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 관심종목 비활성화 (삭제)
     */
    public void deactivate() {
        this.active = false;
        this.removedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ETF 정보 업데이트 (캐시 갱신)
     */
    public void updateEtfInfo(String etfName, String shortCode) {
        this.etfName = etfName;
        this.shortCode = shortCode;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 알림 조건 추가
     */
    public void addAlertCondition(AlertCondition alertCondition) {
        alertConditions.add(alertCondition);
        alertCondition.setWatchList(this);
    }

    /**
     * 알림 조건 제거
     */
    public void removeAlertCondition(AlertCondition alertCondition) {
        alertConditions.remove(alertCondition);
        alertCondition.setWatchList(null);
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
        alert.setWatchList(this);
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
     * 생성자 (새 관심종목 추가용)
     */
    public WatchList(User user, String isinCd, String etfName, String shortCode) {
        this.user = user;
        this.isinCd = isinCd;
        this.etfName = etfName;
        this.shortCode = shortCode;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
