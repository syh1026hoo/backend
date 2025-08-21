package com.example.etfstock.repository;

import com.example.etfstock.entity.AlertCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 조건 Repository
 * 전략과제 #3: 관심종목 알림 조건 관리를 위한 데이터 접근 계층
 */
@Repository
public interface AlertConditionRepository extends JpaRepository<AlertCondition, Long> {

    /**
     * 사용자별 활성 알림 조건 조회
     */
    List<AlertCondition> findByUserIdAndActiveTrue(Long userId);

    /**
     * 사용자별 활성 알림 조건 조회 (페이징)
     */
    Page<AlertCondition> findByUserIdAndActiveTrue(Long userId, Pageable pageable);

    /**
     * 관심종목별 활성 알림 조건 조회
     */
    List<AlertCondition> findByWatchListIdAndActiveTrue(Long watchListId);

    /**
     * 특정 관심종목의 특정 조건 타입 알림 조건 조회
     */
    List<AlertCondition> findByWatchListIdAndConditionTypeAndActiveTrue(Long watchListId, AlertCondition.ConditionType conditionType);

    /**
     * 사용자별 전체 알림 조건 조회 (활성/비활성 포함)
     */
    List<AlertCondition> findByUserId(Long userId);

    /**
     * 특정 ETF 종목의 모든 활성 알림 조건 조회
     */
    @Query("SELECT ac FROM AlertCondition ac JOIN ac.watchList w WHERE w.isinCd = :isinCd AND ac.active = true")
    List<AlertCondition> findActiveConditionsByIsinCd(@Param("isinCd") String isinCd);

    /**
     * 가격 변동 감시가 필요한 모든 활성 알림 조건 조회
     */
    @Query("SELECT ac FROM AlertCondition ac JOIN FETCH ac.watchList w JOIN FETCH ac.user u " +
           "WHERE ac.active = true AND w.active = true AND w.notificationEnabled = true")
    List<AlertCondition> findAllActiveConditionsForMonitoring();

    /**
     * 특정 조건 타입의 활성 알림 조건 조회
     */
    List<AlertCondition> findByConditionTypeAndActiveTrue(AlertCondition.ConditionType conditionType);

    /**
     * 사용자별 활성 알림 조건 개수
     */
    Long countByUserIdAndActiveTrue(Long userId);

    /**
     * 관심종목별 활성 알림 조건 개수
     */
    Long countByWatchListIdAndActiveTrue(Long watchListId);

    /**
     * 특정 기간 이후 마지막으로 트리거된 알림 조건 조회 (중복 방지용)
     */
    @Query("SELECT ac FROM AlertCondition ac WHERE ac.lastTriggeredAt > :afterTime AND ac.active = true")
    List<AlertCondition> findRecentlyTriggeredConditions(@Param("afterTime") LocalDateTime afterTime);

    /**
     * 마지막 트리거 이후 일정 시간이 지난 알림 조건 조회 (재알림 가능한 조건들)
     */
    @Query("SELECT ac FROM AlertCondition ac WHERE " +
           "(ac.lastTriggeredAt IS NULL OR ac.lastTriggeredAt < :beforeTime) " +
           "AND ac.active = true")
    List<AlertCondition> findConditionsReadyForRetrigger(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 특정 사용자의 특정 관심종목에 대한 특정 타입의 알림 조건 존재 여부
     */
    boolean existsByUserIdAndWatchListIdAndConditionTypeAndActiveTrue(Long userId, Long watchListId, AlertCondition.ConditionType conditionType);

    /**
     * 사용자와 조건 타입별 알림 조건 조회
     */
    @Query("SELECT ac FROM AlertCondition ac WHERE ac.user.id = :userId AND ac.conditionType = :conditionType AND ac.active = true")
    List<AlertCondition> findByUserAndConditionType(@Param("userId") Long userId, @Param("conditionType") AlertCondition.ConditionType conditionType);

    /**
     * 특정 ETF를 감시하는 사용자들의 알림 조건 조회
     */
    @Query("SELECT ac FROM AlertCondition ac JOIN ac.watchList w WHERE w.isinCd = :isinCd AND ac.active = true AND w.active = true")
    List<AlertCondition> findActiveConditionsByEtfCode(@Param("isinCd") String isinCd);

    /**
     * 임계값 범위로 알림 조건 조회 (특정 변동률 범위의 조건들)
     */
    @Query("SELECT ac FROM AlertCondition ac WHERE ac.conditionType = :conditionType " +
           "AND ac.thresholdValue BETWEEN :minThreshold AND :maxThreshold " +
           "AND ac.active = true")
    List<AlertCondition> findByConditionTypeAndThresholdRange(
        @Param("conditionType") AlertCondition.ConditionType conditionType,
        @Param("minThreshold") java.math.BigDecimal minThreshold,
        @Param("maxThreshold") java.math.BigDecimal maxThreshold
    );

    /**
     * 오래된 비활성 알림 조건 삭제 (30일 이상)
     */
    @Query("DELETE FROM AlertCondition ac WHERE ac.active = false AND ac.updatedAt < :cutoffDate")
    void deleteInactiveConditionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 알림 조건 통계 (타입별 개수)
     */
    @Query("SELECT ac.conditionType, COUNT(ac) FROM AlertCondition ac WHERE ac.active = true GROUP BY ac.conditionType")
    List<Object[]> getConditionTypeStatistics();

    /**
     * 사용자별 알림 조건 통계
     */
    @Query("SELECT ac.user.id, COUNT(ac) as conditionCount FROM AlertCondition ac WHERE ac.active = true GROUP BY ac.user.id")
    List<Object[]> getUserConditionStatistics();

    /**
     * 최근 생성된 알림 조건 조회
     */
    @Query("SELECT ac FROM AlertCondition ac WHERE ac.createdAt >= :fromDate AND ac.active = true ORDER BY ac.createdAt DESC")
    List<AlertCondition> findRecentConditions(@Param("fromDate") LocalDateTime fromDate, Pageable pageable);

    /**
     * 특정 관심종목에 대한 사용자의 알림 조건 조회
     */
    Optional<AlertCondition> findByUserIdAndWatchListIdAndActiveTrue(Long userId, Long watchListId);

    /**
     * 알림이 비활성화된 관심종목의 알림 조건들 비활성화
     */
    @Query("UPDATE AlertCondition ac SET ac.active = false, ac.updatedAt = :now " +
           "WHERE ac.watchList.id = :watchListId AND (ac.watchList.notificationEnabled = false OR ac.watchList.active = false)")
    void deactivateConditionsForDisabledWatchList(@Param("watchListId") Long watchListId, @Param("now") LocalDateTime now);
}
