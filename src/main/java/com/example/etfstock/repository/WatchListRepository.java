package com.example.etfstock.repository;

import com.example.etfstock.entity.WatchList;
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
 * 관심종목 Repository
 * 전략과제 #2: 관심종목 관리를 위한 데이터 접근 계층
 */
@Repository
public interface WatchListRepository extends JpaRepository<WatchList, Long> {

    /**
     * 사용자별 활성 관심종목 조회
     */
    List<WatchList> findByUserIdAndActiveTrue(Long userId);

    /**
     * 사용자별 활성 관심종목 조회 (페이징)
     */
    Page<WatchList> findByUserIdAndActiveTrue(Long userId, Pageable pageable);

    /**
     * 사용자별 전체 관심종목 조회 (활성/비활성 포함)
     */
    List<WatchList> findByUserId(Long userId);

    /**
     * 특정 사용자의 특정 ETF 관심종목 조회
     */
    Optional<WatchList> findByUserIdAndIsinCd(Long userId, String isinCd);

    /**
     * 특정 사용자의 특정 ETF 활성 관심종목 조회
     */
    Optional<WatchList> findByUserIdAndIsinCdAndActiveTrue(Long userId, String isinCd);

    /**
     * 특정 ETF의 활성 관심종목 수 조회 (좋아요 수)
     */
    @Query("SELECT COUNT(w) FROM WatchList w WHERE w.isinCd = :isinCd AND w.active = true")
    Long countByIsinCdAndActiveTrue(@Param("isinCd") String isinCd);

    /**
     * 특정 ETF를 관심종목으로 등록한 사용자 목록
     */
    @Query("SELECT w FROM WatchList w JOIN FETCH w.user WHERE w.isinCd = :isinCd AND w.active = true")
    List<WatchList> findActiveWatchListsByIsinCd(@Param("isinCd") String isinCd);

    /**
     * 사용자별 활성 관심종목 개수
     */
    Long countByUserIdAndActiveTrue(Long userId);

    /**
     * 관심종목 인기 순위 (ETF별 좋아요 수 내림차순)
     */
    @Query("SELECT w.isinCd, w.etfName, COUNT(w) as likeCount " +
           "FROM WatchList w WHERE w.active = true " +
           "GROUP BY w.isinCd, w.etfName " +
           "ORDER BY likeCount DESC")
    List<Object[]> findEtfPopularityRanking();

    /**
     * 관심종목 인기 순위 (상위 N개)
     */
    @Query("SELECT w.isinCd, w.etfName, COUNT(w) as likeCount " +
           "FROM WatchList w WHERE w.active = true " +
           "GROUP BY w.isinCd, w.etfName " +
           "ORDER BY likeCount DESC")
    List<Object[]> findTopEtfsByLikes(Pageable pageable);

    /**
     * 특정 기간 내 추가된 관심종목 조회
     */
    @Query("SELECT w FROM WatchList w WHERE w.createdAt BETWEEN :startDate AND :endDate AND w.active = true")
    List<WatchList> findByCreatedAtBetweenAndActiveTrue(@Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 최근 추가한 관심종목 조회
     */
    @Query("SELECT w FROM WatchList w WHERE w.user.id = :userId AND w.active = true ORDER BY w.createdAt DESC")
    List<WatchList> findRecentWatchListsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 전체 관심종목 통계
     */
    @Query("SELECT COUNT(DISTINCT w.user.id) as totalUsers, " +
           "COUNT(DISTINCT w.isinCd) as totalEtfs, " +
           "COUNT(w) as totalWatchLists " +
           "FROM WatchList w WHERE w.active = true")
    Object[] getWatchListStatistics();

    /**
     * ETF별 좋아요 수 조회 (여러 ETF 한번에)
     */
    @Query("SELECT w.isinCd, COUNT(w) as likeCount " +
           "FROM WatchList w WHERE w.isinCd IN :isinCds AND w.active = true " +
           "GROUP BY w.isinCd")
    List<Object[]> countLikesByIsinCds(@Param("isinCds") List<String> isinCds);

    /**
     * 특정 사용자가 관심종목으로 등록했는지 확인
     */
    boolean existsByUserIdAndIsinCdAndActiveTrue(Long userId, String isinCd);

    /**
     * 사용자별 관심종목에서 ETF명으로 검색
     */
    @Query("SELECT w FROM WatchList w WHERE w.user.id = :userId AND w.active = true " +
           "AND (LOWER(w.etfName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.shortCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<WatchList> searchUserWatchLists(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 알림 설정이 활성화된 관심종목 조회
     */
    @Query("SELECT w FROM WatchList w WHERE w.user.id = :userId AND w.active = true AND w.notificationEnabled = true")
    List<WatchList> findByUserIdAndActiveTrueAndNotificationEnabledTrue(@Param("userId") Long userId);

    /**
     * 비활성화된 관심종목 정리 (30일 이상 된 것들)
     */
    @Query("DELETE FROM WatchList w WHERE w.active = false AND w.removedAt < :cutoffDate")
    void deleteInactiveWatchListsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
