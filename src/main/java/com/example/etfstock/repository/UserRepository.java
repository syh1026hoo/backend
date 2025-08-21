package com.example.etfstock.repository;

import com.example.etfstock.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 Repository
 * 전략과제 #2: 사용자 관리를 위한 데이터 접근 계층
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 사용자명 또는 이메일로 사용자 조회
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 상태별 조회
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 활성 사용자 목록 조회
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE'")
    List<User> findActiveUsers();

    /**
     * 특정 기간 내 생성된 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 기간 내 로그인한 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate")
    List<User> findByLastLoginAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 관심종목이 있는 사용자 조회
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.watchLists w WHERE w.active = true")
    List<User> findUsersWithActiveWatchLists();

    /**
     * 특정 ETF를 관심종목으로 등록한 사용자 조회
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.watchLists w WHERE w.isinCd = :isinCd AND w.active = true")
    List<User> findUsersByWatchListEtf(@Param("isinCd") String isinCd);

    /**
     * 관심종목 개수별 사용자 통계
     */
    @Query("SELECT u.id, u.username, COUNT(w) as watchListCount " +
           "FROM User u LEFT JOIN u.watchLists w ON w.active = true " +
           "GROUP BY u.id, u.username " +
           "ORDER BY watchListCount DESC")
    List<Object[]> findUsersWithWatchListCount();

    /**
     * 사용자명으로 유사 검색 (자동완성용)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) AND u.status = 'ACTIVE'")
    List<User> findByUsernameContainingIgnoreCase(@Param("keyword") String keyword);

    /**
     * 이름으로 유사 검색
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND u.status = 'ACTIVE'")
    List<User> findByFullNameContainingIgnoreCase(@Param("keyword") String keyword);
}
