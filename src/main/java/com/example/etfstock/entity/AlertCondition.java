package com.example.etfstock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 알림 조건 엔티티
 * 전략과제 #3: 관심종목 알림 시스템의 조건 설정
 */
@Entity
@Table(name = "alert_conditions", 
       indexes = {
           @Index(name = "idx_alert_condition_watchlist", columnList = "watchlist_id"),
           @Index(name = "idx_alert_condition_active", columnList = "is_active"),
           @Index(name = "idx_alert_condition_type", columnList = "condition_type"),
           @Index(name = "idx_alert_condition_user", columnList = "user_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림 조건이 적용될 관심종목
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private WatchList watchList;

    /**
     * 사용자 (조회 성능을 위한 비정규화)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 알림 조건 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 20)
    private ConditionType conditionType;

    /**
     * 기준값 (예: -3.0 = 3% 하락, 5.0 = 5% 상승)
     */
    @Column(name = "threshold_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal thresholdValue;

    /**
     * 기준 가격 (알림 조건 설정 시점의 가격)
     */
    @Column(name = "base_price", precision = 15, scale = 2)
    private BigDecimal basePrice;

    /**
     * 알림 조건 활성 상태
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * 알림 조건 생성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 수정일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 알림 조건 설명 (사용자가 입력한 메모)
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 마지막 알림 발생일시 (중복 알림 방지용)
     */
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    /**
     * 알림 조건 타입 열거형
     */
    public enum ConditionType {
        PRICE_DROP("가격 하락"),           // 절대 가격 하락
        PRICE_RISE("가격 상승"),           // 절대 가격 상승
        PERCENTAGE_DROP("비율 하락"),      // 퍼센트 하락 (주로 사용)
        PERCENTAGE_RISE("비율 상승"),      // 퍼센트 상승
        VOLUME_SPIKE("거래량 급증"),       // 거래량 급증 (향후 확장)
        PRICE_TARGET("목표 가격");         // 특정 가격 도달

        private final String description;

        ConditionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

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
     * 알림 조건 활성화
     */
    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 알림 조건 비활성화
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 알림 발생 시점 업데이트
     */
    public void updateLastTriggered() {
        this.lastTriggeredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 기준 가격 업데이트
     */
    public void updateBasePrice(BigDecimal newBasePrice) {
        this.basePrice = newBasePrice;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 생성자 (새 알림 조건 생성용)
     */
    public AlertCondition(WatchList watchList, User user, ConditionType conditionType, 
                         BigDecimal thresholdValue, BigDecimal basePrice, String description) {
        this.watchList = watchList;
        this.user = user;
        this.conditionType = conditionType;
        this.thresholdValue = thresholdValue;
        this.basePrice = basePrice;
        this.description = description;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
