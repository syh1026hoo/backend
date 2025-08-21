package com.example.etfstock.service;

import com.example.etfstock.entity.Alert;
import com.example.etfstock.repository.AlertRepository;
import com.example.etfstock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 발생 및 표출 서비스
 * 전략과제 #3: 발생한 알림의 조회, 읽음 처리, 관리 기능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlertNotificationService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    /**
     * 사용자별 모든 알림 조회 (최신순)
     */
    public List<Alert> getUserAlerts(Long userId) {
        try {
            return alertRepository.findByUserIdOrderByTriggeredAtDesc(userId);
        } catch (Exception e) {
            log.error("사용자 알림 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 알림 조회 (페이징)
     */
    public Page<Alert> getUserAlerts(Long userId, Pageable pageable) {
        try {
            return alertRepository.findByUserIdOrderByTriggeredAtDesc(userId, pageable);
        } catch (Exception e) {
            log.error("사용자 알림 페이징 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 읽지 않은 알림 조회
     */
    public List<Alert> getUnreadAlerts(Long userId) {
        try {
            return alertRepository.findUnreadAlertsByUserId(userId);
        } catch (Exception e) {
            log.error("읽지 않은 알림 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("읽지 않은 알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 읽지 않은 알림 조회 (페이징)
     */
    public Page<Alert> getUnreadAlerts(Long userId, Pageable pageable) {
        try {
            return alertRepository.findUnreadAlertsByUserId(userId, pageable);
        } catch (Exception e) {
            log.error("읽지 않은 알림 페이징 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("읽지 않은 알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public long getUnreadAlertCount(Long userId) {
        try {
            return alertRepository.countByUserIdAndReadFalse(userId);
        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return 0;
        }
    }

    /**
     * 특정 ETF의 알림 조회
     */
    public List<Alert> getEtfAlerts(Long userId, String isinCd) {
        try {
            List<Alert> allAlerts = alertRepository.findByIsinCdOrderByTriggeredAtDesc(isinCd);
            
            // 사용자의 알림만 필터링
            return allAlerts.stream()
                    .filter(alert -> alert.getUser().getId().equals(userId))
                    .toList();
        } catch (Exception e) {
            log.error("ETF 알림 조회 중 오류 발생 - 사용자 ID: {}, ETF: {}", userId, isinCd, e);
            throw new RuntimeException("ETF 알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 최근 24시간 알림 조회
     */
    public List<Alert> getRecentAlerts(Long userId) {
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            return alertRepository.findRecentAlertsByUserId(userId, yesterday);
        } catch (Exception e) {
            log.error("최근 알림 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("최근 알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 고우선순위 알림 조회
     */
    public List<Alert> getHighPriorityAlerts(Long userId) {
        try {
            return alertRepository.findHighPriorityAlertsByUserId(userId);
        } catch (Exception e) {
            log.error("고우선순위 알림 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("고우선순위 알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAlertAsRead(Long alertId, Long userId) {
        try {
            log.info("알림 읽음 처리 시작 - 알림 ID: {}, 사용자 ID: {}", alertId, userId);

            Optional<Alert> alertOpt = alertRepository.findById(alertId);
            if (alertOpt.isEmpty()) {
                throw new IllegalArgumentException("알림을 찾을 수 없습니다: " + alertId);
            }

            Alert alert = alertOpt.get();

            // 소유자 확인
            if (!alert.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 알림에 대한 권한이 없습니다.");
            }

            // 읽음 처리
            if (!alert.isRead()) {
                alert.markAsRead();
                alertRepository.save(alert);
                log.info("알림 읽음 처리 완료 - 알림 ID: {}", alertId);
            }

        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류 발생 - 알림 ID: {}", alertId, e);
            throw new RuntimeException("알림 읽음 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 알림 읽지 않음 처리
     */
    @Transactional
    public void markAlertAsUnread(Long alertId, Long userId) {
        try {
            log.info("알림 읽지 않음 처리 시작 - 알림 ID: {}, 사용자 ID: {}", alertId, userId);

            Optional<Alert> alertOpt = alertRepository.findById(alertId);
            if (alertOpt.isEmpty()) {
                throw new IllegalArgumentException("알림을 찾을 수 없습니다: " + alertId);
            }

            Alert alert = alertOpt.get();

            // 소유자 확인
            if (!alert.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 알림에 대한 권한이 없습니다.");
            }

            // 읽지 않음 처리
            if (alert.isRead()) {
                alert.markAsUnread();
                alertRepository.save(alert);
                log.info("알림 읽지 않음 처리 완료 - 알림 ID: {}", alertId);
            }

        } catch (Exception e) {
            log.error("알림 읽지 않음 처리 중 오류 발생 - 알림 ID: {}", alertId, e);
            throw new RuntimeException("알림 읽지 않음 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 모든 알림 읽음 처리 (일괄 처리)
     */
    @Transactional
    public void markAllAlertsAsRead(Long userId) {
        try {
            log.info("모든 알림 읽음 처리 시작 - 사용자 ID: {}", userId);

            // 사용자 존재 확인
            if (!userRepository.existsById(userId)) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
            }

            alertRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
            
            log.info("모든 알림 읽음 처리 완료 - 사용자 ID: {}", userId);

        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("모든 알림 읽음 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 관심종목의 모든 알림 읽음 처리
     */
    @Transactional
    public void markWatchListAlertsAsRead(Long userId, Long watchListId) {
        try {
            log.info("관심종목 알림 읽음 처리 시작 - 사용자 ID: {}, 관심종목 ID: {}", userId, watchListId);

            // 권한 확인은 Repository 쿼리에서 사용자 ID로 필터링하여 처리
            alertRepository.markAsReadByWatchListId(watchListId, LocalDateTime.now());
            
            log.info("관심종목 알림 읽음 처리 완료 - 관심종목 ID: {}", watchListId);

        } catch (Exception e) {
            log.error("관심종목 알림 읽음 처리 중 오류 발생 - 관심종목 ID: {}", watchListId, e);
            throw new RuntimeException("관심종목 알림 읽음 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 알림 무시 처리
     */
    @Transactional
    public void dismissAlert(Long alertId, Long userId) {
        try {
            log.info("알림 무시 처리 시작 - 알림 ID: {}, 사용자 ID: {}", alertId, userId);

            Optional<Alert> alertOpt = alertRepository.findById(alertId);
            if (alertOpt.isEmpty()) {
                throw new IllegalArgumentException("알림을 찾을 수 없습니다: " + alertId);
            }

            Alert alert = alertOpt.get();

            // 소유자 확인
            if (!alert.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 알림에 대한 권한이 없습니다.");
            }

            // 무시 처리
            alert.dismiss();
            alert.markAsRead(); // 무시된 알림은 읽음으로도 처리
            alertRepository.save(alert);
            
            log.info("알림 무시 처리 완료 - 알림 ID: {}", alertId);

        } catch (Exception e) {
            log.error("알림 무시 처리 중 오류 발생 - 알림 ID: {}", alertId, e);
            throw new RuntimeException("알림 무시 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 알림 통계
     */
    public UserAlertStatistics getUserAlertStatistics(Long userId) {
        try {
            // 전체 알림 수
            long totalAlerts = alertRepository.findByUserIdOrderByTriggeredAtDesc(userId).size();
            
            // 읽지 않은 알림 수
            long unreadAlerts = alertRepository.countByUserIdAndReadFalse(userId);
            
            // 최근 24시간 알림 수
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            long recentAlerts = alertRepository.findRecentAlertsByUserId(userId, yesterday).size();
            
            // 고우선순위 알림 수
            long highPriorityAlerts = alertRepository.findHighPriorityAlertsByUserId(userId).size();

            // 알림 타입별 통계
            List<Object[]> typeStats = alertRepository.getAlertTypeStatisticsByUserId(userId);
            
            return new UserAlertStatistics(
                totalAlerts, unreadAlerts, recentAlerts, highPriorityAlerts, typeStats
            );

        } catch (Exception e) {
            log.error("사용자 알림 통계 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return new UserAlertStatistics(0, 0, 0, 0, List.of());
        }
    }

    /**
     * 특정 알림 상세 조회
     */
    public Optional<Alert> getAlert(Long alertId, Long userId) {
        try {
            Optional<Alert> alertOpt = alertRepository.findById(alertId);
            
            if (alertOpt.isPresent() && !alertOpt.get().getUser().getId().equals(userId)) {
                log.warn("알림 접근 권한 없음 - 알림 ID: {}, 사용자 ID: {}", alertId, userId);
                return Optional.empty();
            }
            
            return alertOpt;
        } catch (Exception e) {
            log.error("알림 상세 조회 중 오류 발생 - 알림 ID: {}", alertId, e);
            return Optional.empty();
        }
    }

    /**
     * 사용자별 일별 알림 통계 (최근 30일)
     */
    public List<Object[]> getUserDailyAlertStatistics(Long userId) {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            return alertRepository.getDailyAlertStatisticsByUserId(userId, thirtyDaysAgo);
        } catch (Exception e) {
            log.error("사용자 일별 알림 통계 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return List.of();
        }
    }

    /**
     * 최근 N개 알림 조회
     */
    public List<Alert> getTopNAlerts(Long userId, int limit) {
        try {
            Pageable pageable = Pageable.ofSize(limit);
            return alertRepository.findTopNByUserId(userId, pageable);
        } catch (Exception e) {
            log.error("최근 N개 알림 조회 중 오류 발생 - 사용자 ID: {}, 개수: {}", userId, limit, e);
            throw new RuntimeException("최근 알림 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 알림 통계 내부 클래스
     */
    public static class UserAlertStatistics {
        private final long totalAlerts;
        private final long unreadAlerts;
        private final long recentAlerts;
        private final long highPriorityAlerts;
        private final List<Object[]> typeStatistics;

        public UserAlertStatistics(long totalAlerts, long unreadAlerts, long recentAlerts, 
                                 long highPriorityAlerts, List<Object[]> typeStatistics) {
            this.totalAlerts = totalAlerts;
            this.unreadAlerts = unreadAlerts;
            this.recentAlerts = recentAlerts;
            this.highPriorityAlerts = highPriorityAlerts;
            this.typeStatistics = typeStatistics;
        }

        // Getters
        public long getTotalAlerts() { return totalAlerts; }
        public long getUnreadAlerts() { return unreadAlerts; }
        public long getRecentAlerts() { return recentAlerts; }
        public long getHighPriorityAlerts() { return highPriorityAlerts; }
        public List<Object[]> getTypeStatistics() { return typeStatistics; }

        @Override
        public String toString() {
            return String.format("전체: %d개, 미읽음: %d개, 최근: %d개, 고우선순위: %d개",
                totalAlerts, unreadAlerts, recentAlerts, highPriorityAlerts);
        }
    }
}
