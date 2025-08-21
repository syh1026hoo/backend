package com.example.etfstock.repository;

import com.example.etfstock.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 이력 Repository
 * 전략과제 #3: 발생한 알림 기록 관리를 위한 데이터 접근 계층
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 사용자별 알림 조회 (최신순)
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId ORDER BY a.triggeredAt DESC")
    List<Alert> findByUserIdOrderByTriggeredAtDesc(@Param("userId") Long userId);

    /**
     * 사용자별 알림 조회 (페이징)
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId ORDER BY a.triggeredAt DESC")
    Page<Alert> findByUserIdOrderByTriggeredAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.read = false ORDER BY a.triggeredAt DESC")
    List<Alert> findUnreadAlertsByUserId(@Param("userId") Long userId);

    /**
     * 사용자별 읽지 않은 알림 조회 (페이징)
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.read = false ORDER BY a.triggeredAt DESC")
    Page<Alert> findUnreadAlertsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 개수
     */
    Long countByUserIdAndReadFalse(Long userId);

    /**
     * 특정 관심종목의 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.watchList.id = :watchListId ORDER BY a.triggeredAt DESC")
    List<Alert> findByWatchListIdOrderByTriggeredAtDesc(@Param("watchListId") Long watchListId);

    /**
     * 특정 ETF 종목의 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.isinCd = :isinCd ORDER BY a.triggeredAt DESC")
    List<Alert> findByIsinCdOrderByTriggeredAtDesc(@Param("isinCd") String isinCd);

    /**
     * 특정 알림 조건으로 발생한 알림들 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.alertCondition.id = :conditionId ORDER BY a.triggeredAt DESC")
    List<Alert> findByAlertConditionIdOrderByTriggeredAtDesc(@Param("conditionId") Long conditionId);

    /**
     * 특정 기간 내 발생한 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.triggeredAt BETWEEN :startDate AND :endDate ORDER BY a.triggeredAt DESC")
    List<Alert> findByTriggeredAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 특정 기간 내 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.triggeredAt BETWEEN :startDate AND :endDate ORDER BY a.triggeredAt DESC")
    List<Alert> findByUserIdAndTriggeredAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 알림 타입별 조회
     */
    List<Alert> findByAlertTypeOrderByTriggeredAtDesc(Alert.AlertType alertType);

    /**
     * 사용자별 알림 타입별 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.alertType = :alertType ORDER BY a.triggeredAt DESC")
    List<Alert> findByUserIdAndAlertType(@Param("userId") Long userId, @Param("alertType") Alert.AlertType alertType);

    /**
     * 우선순위별 알림 조회
     */
    List<Alert> findByPriorityOrderByTriggeredAtDesc(Alert.Priority priority);

    /**
     * 사용자별 고우선순위 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.priority IN ('HIGH', 'URGENT') ORDER BY a.triggeredAt DESC")
    List<Alert> findHighPriorityAlertsByUserId(@Param("userId") Long userId);

    /**
     * 활성 상태별 알림 조회
     */
    List<Alert> findByAlertStatusOrderByTriggeredAtDesc(Alert.AlertStatus alertStatus);

    /**
     * 최근 24시간 내 발생한 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.triggeredAt > :yesterday ORDER BY a.triggeredAt DESC")
    List<Alert> findRecentAlerts(@Param("yesterday") LocalDateTime yesterday);

    /**
     * 사용자별 최근 24시간 내 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.triggeredAt > :yesterday ORDER BY a.triggeredAt DESC")
    List<Alert> findRecentAlertsByUserId(@Param("userId") Long userId, @Param("yesterday") LocalDateTime yesterday);

    /**
     * 특정 ETF의 최근 알림 조회 (중복 알림 방지용)
     */
    @Query("SELECT a FROM Alert a WHERE a.isinCd = :isinCd AND a.triggeredAt > :afterTime ORDER BY a.triggeredAt DESC")
    List<Alert> findRecentAlertsByEtfCode(@Param("isinCd") String isinCd, @Param("afterTime") LocalDateTime afterTime);

    /**
     * 사용자별 알림 통계 (타입별 개수)
     */
    @Query("SELECT a.alertType, COUNT(a) FROM Alert a WHERE a.user.id = :userId GROUP BY a.alertType")
    List<Object[]> getAlertTypeStatisticsByUserId(@Param("userId") Long userId);

    /**
     * 전체 알림 통계
     */
    @Query("SELECT COUNT(a) as totalAlerts, " +
           "COUNT(CASE WHEN a.read = false THEN 1 END) as unreadAlerts, " +
           "COUNT(CASE WHEN a.priority IN ('HIGH', 'URGENT') THEN 1 END) as highPriorityAlerts " +
           "FROM Alert a")
    Object[] getGlobalAlertStatistics();

    /**
     * ETF별 알림 발생 빈도 통계
     */
    @Query("SELECT a.isinCd, a.etfName, COUNT(a) as alertCount FROM Alert a GROUP BY a.isinCd, a.etfName ORDER BY alertCount DESC")
    List<Object[]> getEtfAlertFrequencyStatistics();

    /**
     * 일별 알림 발생 통계
     */
    @Query("SELECT DATE(a.triggeredAt) as alertDate, COUNT(a) as alertCount " +
           "FROM Alert a WHERE a.triggeredAt >= :fromDate " +
           "GROUP BY DATE(a.triggeredAt) ORDER BY alertDate DESC")
    List<Object[]> getDailyAlertStatistics(@Param("fromDate") LocalDateTime fromDate);

    /**
     * 사용자별 일별 알림 발생 통계
     */
    @Query("SELECT DATE(a.triggeredAt) as alertDate, COUNT(a) as alertCount " +
           "FROM Alert a WHERE a.user.id = :userId AND a.triggeredAt >= :fromDate " +
           "GROUP BY DATE(a.triggeredAt) ORDER BY alertDate DESC")
    List<Object[]> getDailyAlertStatisticsByUserId(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    /**
     * 오래된 읽은 알림 삭제 (30일 이상)
     */
    @Query("DELETE FROM Alert a WHERE a.read = true AND a.readAt < :cutoffDate")
    void deleteOldReadAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 만료된 알림 삭제
     */
    @Query("DELETE FROM Alert a WHERE a.alertStatus = 'EXPIRED'")
    void deleteExpiredAlerts();

    /**
     * 특정 사용자의 특정 ETF에 대한 최근 알림 (중복 방지용)
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.isinCd = :isinCd " +
           "AND a.alertType = :alertType AND a.triggeredAt > :afterTime " +
           "ORDER BY a.triggeredAt DESC")
    List<Alert> findRecentSimilarAlerts(@Param("userId") Long userId, 
                                       @Param("isinCd") String isinCd,
                                       @Param("alertType") Alert.AlertType alertType,
                                       @Param("afterTime") LocalDateTime afterTime);

    /**
     * 대량 알림 읽음 처리
     */
    @Query("UPDATE Alert a SET a.read = true, a.readAt = :readAt WHERE a.user.id = :userId AND a.read = false")
    void markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * 특정 관심종목의 알림들 읽음 처리
     */
    @Query("UPDATE Alert a SET a.read = true, a.readAt = :readAt WHERE a.watchList.id = :watchListId AND a.read = false")
    void markAsReadByWatchListId(@Param("watchListId") Long watchListId, @Param("readAt") LocalDateTime readAt);

    /**
     * 사용자별 최근 N개 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId ORDER BY a.triggeredAt DESC")
    List<Alert> findTopNByUserId(@Param("userId") Long userId, Pageable pageable);
}
