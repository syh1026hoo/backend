package com.example.etfstock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 알림 이력 엔티티
 * 전략과제 #3: 실제 발생한 알림 기록 관리
 */
@Entity
@Table(name = "alerts", 
       indexes = {
           @Index(name = "idx_alert_user", columnList = "user_id"),
           @Index(name = "idx_alert_condition", columnList = "alert_condition_id"),
           @Index(name = "idx_alert_watchlist", columnList = "watchlist_id"),
           @Index(name = "idx_alert_triggered", columnList = "triggered_at"),
           @Index(name = "idx_alert_read", columnList = "is_read"),
           @Index(name = "idx_alert_status", columnList = "alert_status"),
           @Index(name = "idx_alert_isin", columnList = "isin_cd")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 발생시킨 조건
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_condition_id", nullable = false)
    private AlertCondition alertCondition;

    /**
     * 관련 관심종목
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
     * ETF 종목 코드 (조회 성능을 위한 비정규화)
     */
    @Column(name = "isin_cd", nullable = false, length = 12)
    private String isinCd;

    /**
     * ETF 종목명 (조회 성능을 위한 비정규화)
     */
    @Column(name = "etf_name", nullable = false, length = 200)
    private String etfName;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    /**
     * 알림 제목
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 알림 메시지
     */
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    /**
     * 알림 발생 시점의 가격
     */
    @Column(name = "trigger_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal triggerPrice;

    /**
     * 기준 가격 (비교 대상 가격)
     */
    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    /**
     * 변동률 (%)
     */
    @Column(name = "change_percentage", nullable = false, precision = 10, scale = 4)
    private BigDecimal changePercentage;

    /**
     * 변동 금액
     */
    @Column(name = "change_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal changeAmount;

    /**
     * 알림 발생일시
     */
    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    /**
     * 알림 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /**
     * 알림 읽은 일시
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 알림 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false, length = 20)
    private AlertStatus alertStatus = AlertStatus.ACTIVE;

    /**
     * 알림 우선순위
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority = Priority.NORMAL;

    /**
     * 생성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 알림 타입 열거형
     */
    public enum AlertType {
        PRICE_DROP("가격 하락"),
        PRICE_RISE("가격 상승"),
        PERCENTAGE_DROP("비율 하락"),
        PERCENTAGE_RISE("비율 상승"),
        VOLUME_SPIKE("거래량 급증"),
        PRICE_TARGET("목표 가격 도달");

        private final String description;

        AlertType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 알림 상태 열거형
     */
    public enum AlertStatus {
        ACTIVE("활성"),           // 활성 알림
        DISMISSED("무시됨"),      // 사용자가 무시한 알림
        EXPIRED("만료됨");        // 만료된 알림

        private final String description;

        AlertStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 우선순위 열거형
     */
    public enum Priority {
        LOW("낮음"),
        NORMAL("보통"),
        HIGH("높음"),
        URGENT("긴급");

        private final String description;

        Priority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) {
            triggeredAt = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
    }

    /**
     * 알림을 읽음으로 표시
     */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 알림을 읽지 않음으로 표시
     */
    public void markAsUnread() {
        this.read = false;
        this.readAt = null;
    }

    /**
     * 알림 무시
     */
    public void dismiss() {
        this.alertStatus = AlertStatus.DISMISSED;
    }

    /**
     * 알림 만료
     */
    public void expire() {
        this.alertStatus = AlertStatus.EXPIRED;
    }

    /**
     * 우선순위 설정
     */
    public void setPriorityByChangePercentage(BigDecimal changePercentage) {
        BigDecimal absChange = changePercentage.abs();
        if (absChange.compareTo(BigDecimal.valueOf(10)) >= 0) {
            this.priority = Priority.URGENT;
        } else if (absChange.compareTo(BigDecimal.valueOf(5)) >= 0) {
            this.priority = Priority.HIGH;
        } else if (absChange.compareTo(BigDecimal.valueOf(2)) >= 0) {
            this.priority = Priority.NORMAL;
        } else {
            this.priority = Priority.LOW;
        }
    }

    /**
     * 생성자 (새 알림 생성용)
     */
    public Alert(AlertCondition alertCondition, WatchList watchList, User user,
                String title, String message, BigDecimal triggerPrice, BigDecimal basePrice,
                BigDecimal changePercentage, BigDecimal changeAmount, AlertType alertType) {
        this.alertCondition = alertCondition;
        this.watchList = watchList;
        this.user = user;
        this.isinCd = watchList.getIsinCd();
        this.etfName = watchList.getEtfName();
        this.title = title;
        this.message = message;
        this.triggerPrice = triggerPrice;
        this.basePrice = basePrice;
        this.changePercentage = changePercentage;
        this.changeAmount = changeAmount;
        this.alertType = alertType;
        this.triggeredAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        
        // 변동률에 따른 우선순위 자동 설정
        setPriorityByChangePercentage(changePercentage);
    }
}
