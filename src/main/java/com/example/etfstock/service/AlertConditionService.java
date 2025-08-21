package com.example.etfstock.service;

import com.example.etfstock.dto.EtfSummaryDto;
import com.example.etfstock.entity.AlertCondition;
import com.example.etfstock.entity.User;
import com.example.etfstock.entity.WatchList;
import com.example.etfstock.repository.AlertConditionRepository;
import com.example.etfstock.repository.UserRepository;
import com.example.etfstock.repository.WatchListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 알림 조건 설정/관리 서비스
 * 전략과제 #3: 사용자별 알림 조건 CRUD 및 관리 기능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlertConditionService {

    private final AlertConditionRepository alertConditionRepository;
    private final WatchListRepository watchListRepository;
    private final UserRepository userRepository;
    private final EtfDataService etfDataService;

    /**
     * 알림 조건 생성
     */
    @Transactional
    public AlertCondition createAlertCondition(Long userId, String isinCd, 
                                             AlertCondition.ConditionType conditionType,
                                             BigDecimal thresholdValue, String description) {
        try {
            log.info("알림 조건 생성 시작 - 사용자 ID: {}, ETF: {}, 조건: {}, 임계값: {}", 
                userId, isinCd, conditionType, thresholdValue);

            // 사용자 존재 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 관심종목 존재 확인
            WatchList watchList = watchListRepository.findByUserIdAndIsinCdAndActiveTrue(userId, isinCd)
                    .orElseThrow(() -> new IllegalArgumentException("관심종목을 찾을 수 없습니다. 먼저 관심종목으로 등록해주세요."));

            // 동일한 조건이 이미 존재하는지 확인
            if (alertConditionRepository.existsByUserIdAndWatchListIdAndConditionTypeAndActiveTrue(
                    userId, watchList.getId(), conditionType)) {
                throw new IllegalArgumentException("이미 동일한 알림 조건이 설정되어 있습니다.");
            }

            // 현재 ETF 가격 정보 조회하여 기준 가격 설정
            BigDecimal basePrice = getCurrentEtfPrice(isinCd);
            if (basePrice == null) {
                log.warn("현재 가격을 조회할 수 없어 기준 가격을 null로 설정 - ISIN: {}", isinCd);
            }

            // 알림 조건 생성
            AlertCondition alertCondition = new AlertCondition(
                watchList, user, conditionType, thresholdValue, basePrice, description
            );

            AlertCondition saved = alertConditionRepository.save(alertCondition);
            
            // 관심종목에 알림 조건 추가
            watchList.addAlertCondition(saved);
            user.addAlertCondition(saved);

            log.info("알림 조건 생성 완료 - ID: {}, 조건: {}", saved.getId(), conditionType.getDescription());
            return saved;

        } catch (Exception e) {
            log.error("알림 조건 생성 중 오류 발생 - 사용자 ID: {}, ETF: {}", userId, isinCd, e);
            throw new RuntimeException("알림 조건 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 알림 조건 수정
     */
    @Transactional
    public AlertCondition updateAlertCondition(Long conditionId, Long userId,
                                             BigDecimal thresholdValue, String description) {
        try {
            log.info("알림 조건 수정 시작 - 조건 ID: {}, 사용자 ID: {}", conditionId, userId);

            AlertCondition condition = alertConditionRepository.findById(conditionId)
                    .orElseThrow(() -> new IllegalArgumentException("알림 조건을 찾을 수 없습니다: " + conditionId));

            // 소유자 확인
            if (!condition.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 알림 조건에 대한 권한이 없습니다.");
            }

            // 활성 상태 확인
            if (!condition.isActive()) {
                throw new IllegalArgumentException("비활성화된 알림 조건은 수정할 수 없습니다.");
            }

            // 임계값 수정
            if (thresholdValue != null) {
                condition.setThresholdValue(thresholdValue);
            }

            // 설명 수정
            if (description != null) {
                condition.setDescription(description);
            }

            // 기준 가격 재설정 (임계값이 변경된 경우)
            if (thresholdValue != null) {
                BigDecimal newBasePrice = getCurrentEtfPrice(condition.getWatchList().getIsinCd());
                if (newBasePrice != null) {
                    condition.updateBasePrice(newBasePrice);
                }
            }

            AlertCondition updated = alertConditionRepository.save(condition);
            
            log.info("알림 조건 수정 완료 - ID: {}", updated.getId());
            return updated;

        } catch (Exception e) {
            log.error("알림 조건 수정 중 오류 발생 - 조건 ID: {}", conditionId, e);
            throw new RuntimeException("알림 조건 수정 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 알림 조건 삭제 (비활성화)
     */
    @Transactional
    public void deleteAlertCondition(Long conditionId, Long userId) {
        try {
            log.info("알림 조건 삭제 시작 - 조건 ID: {}, 사용자 ID: {}", conditionId, userId);

            AlertCondition condition = alertConditionRepository.findById(conditionId)
                    .orElseThrow(() -> new IllegalArgumentException("알림 조건을 찾을 수 없습니다: " + conditionId));

            // 소유자 확인
            if (!condition.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 알림 조건에 대한 권한이 없습니다.");
            }

            // 비활성화
            condition.deactivate();
            alertConditionRepository.save(condition);

            log.info("알림 조건 삭제 완료 - ID: {}", conditionId);

        } catch (Exception e) {
            log.error("알림 조건 삭제 중 오류 발생 - 조건 ID: {}", conditionId, e);
            throw new RuntimeException("알림 조건 삭제 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 알림 조건 조회
     */
    public List<AlertCondition> getUserAlertConditions(Long userId) {
        try {
            return alertConditionRepository.findByUserIdAndActiveTrue(userId);
        } catch (Exception e) {
            log.error("사용자 알림 조건 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("알림 조건 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 알림 조건 조회 (페이징)
     */
    public Page<AlertCondition> getUserAlertConditions(Long userId, Pageable pageable) {
        try {
            return alertConditionRepository.findByUserIdAndActiveTrue(userId, pageable);
        } catch (Exception e) {
            log.error("사용자 알림 조건 페이징 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            throw new RuntimeException("알림 조건 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 관심종목별 알림 조건 조회
     */
    public List<AlertCondition> getWatchListAlertConditions(Long userId, String isinCd) {
        try {
            WatchList watchList = watchListRepository.findByUserIdAndIsinCdAndActiveTrue(userId, isinCd)
                    .orElseThrow(() -> new IllegalArgumentException("관심종목을 찾을 수 없습니다."));

            return alertConditionRepository.findByWatchListIdAndActiveTrue(watchList.getId());
        } catch (Exception e) {
            log.error("관심종목 알림 조건 조회 중 오류 발생 - 사용자 ID: {}, ETF: {}", userId, isinCd, e);
            throw new RuntimeException("관심종목 알림 조건 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 알림 조건 상세 조회
     */
    public Optional<AlertCondition> getAlertCondition(Long conditionId, Long userId) {
        try {
            Optional<AlertCondition> conditionOpt = alertConditionRepository.findById(conditionId);
            
            if (conditionOpt.isPresent() && !conditionOpt.get().getUser().getId().equals(userId)) {
                log.warn("알림 조건 접근 권한 없음 - 조건 ID: {}, 사용자 ID: {}", conditionId, userId);
                return Optional.empty();
            }
            
            return conditionOpt;
        } catch (Exception e) {
            log.error("알림 조건 상세 조회 중 오류 발생 - 조건 ID: {}", conditionId, e);
            return Optional.empty();
        }
    }

    /**
     * 알림 조건 활성화/비활성화 토글
     */
    @Transactional
    public boolean toggleAlertCondition(Long conditionId, Long userId) {
        try {
            log.info("알림 조건 토글 시작 - 조건 ID: {}, 사용자 ID: {}", conditionId, userId);

            AlertCondition condition = alertConditionRepository.findById(conditionId)
                    .orElseThrow(() -> new IllegalArgumentException("알림 조건을 찾을 수 없습니다: " + conditionId));

            // 소유자 확인
            if (!condition.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("해당 알림 조건에 대한 권한이 없습니다.");
            }

            boolean newStatus = !condition.isActive();
            if (newStatus) {
                condition.activate();
                // 기준 가격 재설정
                BigDecimal newBasePrice = getCurrentEtfPrice(condition.getWatchList().getIsinCd());
                if (newBasePrice != null) {
                    condition.updateBasePrice(newBasePrice);
                }
            } else {
                condition.deactivate();
            }

            alertConditionRepository.save(condition);
            
            log.info("알림 조건 토글 완료 - ID: {}, 활성 상태: {}", conditionId, newStatus);
            return newStatus;

        } catch (Exception e) {
            log.error("알림 조건 토글 중 오류 발생 - 조건 ID: {}", conditionId, e);
            throw new RuntimeException("알림 조건 토글 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 관심종목의 알림 설정 일괄 활성화/비활성화
     */
    @Transactional
    public void toggleWatchListNotifications(Long userId, String isinCd, boolean enabled) {
        try {
            log.info("관심종목 알림 일괄 토글 시작 - 사용자 ID: {}, ETF: {}, 활성화: {}", userId, isinCd, enabled);

            WatchList watchList = watchListRepository.findByUserIdAndIsinCdAndActiveTrue(userId, isinCd)
                    .orElseThrow(() -> new IllegalArgumentException("관심종목을 찾을 수 없습니다."));

            // 관심종목의 알림 활성화 상태 변경
            watchList.setNotificationEnabled(enabled);
            watchListRepository.save(watchList);

            // 해당 관심종목의 모든 알림 조건 활성화/비활성화
            List<AlertCondition> conditions = alertConditionRepository.findByWatchListIdAndActiveTrue(watchList.getId());
            
            for (AlertCondition condition : conditions) {
                if (enabled) {
                    condition.activate();
                    // 활성화 시 기준 가격 재설정
                    BigDecimal newBasePrice = getCurrentEtfPrice(isinCd);
                    if (newBasePrice != null) {
                        condition.updateBasePrice(newBasePrice);
                    }
                } else {
                    condition.deactivate();
                }
                alertConditionRepository.save(condition);
            }

            log.info("관심종목 알림 일괄 토글 완료 - 영향받은 조건 수: {}", conditions.size());

        } catch (Exception e) {
            log.error("관심종목 알림 일괄 토글 중 오류 발생 - 사용자 ID: {}, ETF: {}", userId, isinCd, e);
            throw new RuntimeException("관심종목 알림 설정 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자의 알림 조건 통계
     */
    public AlertConditionStatistics getUserConditionStatistics(Long userId) {
        try {
            List<AlertCondition> userConditions = alertConditionRepository.findByUserId(userId);
            
            long totalConditions = userConditions.size();
            long activeConditions = userConditions.stream()
                    .filter(AlertCondition::isActive)
                    .count();
            
            // 조건 타입별 개수
            long percentageDropCount = userConditions.stream()
                    .filter(c -> c.getConditionType() == AlertCondition.ConditionType.PERCENTAGE_DROP && c.isActive())
                    .count();
            long percentageRiseCount = userConditions.stream()
                    .filter(c -> c.getConditionType() == AlertCondition.ConditionType.PERCENTAGE_RISE && c.isActive())
                    .count();
            long priceTargetCount = userConditions.stream()
                    .filter(c -> c.getConditionType() == AlertCondition.ConditionType.PRICE_TARGET && c.isActive())
                    .count();

            return new AlertConditionStatistics(
                totalConditions, activeConditions,
                percentageDropCount, percentageRiseCount, priceTargetCount
            );

        } catch (Exception e) {
            log.error("사용자 알림 조건 통계 조회 중 오류 발생 - 사용자 ID: {}", userId, e);
            return new AlertConditionStatistics(0, 0, 0, 0, 0);
        }
    }

    /**
     * 알림 조건 유효성 검증
     */
    public boolean validateCondition(AlertCondition.ConditionType conditionType, BigDecimal thresholdValue) {
        try {
            if (thresholdValue == null) {
                return false;
            }

            switch (conditionType) {
                case PERCENTAGE_DROP:
                    // 하락률은 음수여야 함 (-1% ~ -50%)
                    return thresholdValue.compareTo(BigDecimal.ZERO) < 0 && 
                           thresholdValue.compareTo(BigDecimal.valueOf(-50)) >= 0;
                    
                case PERCENTAGE_RISE:
                    // 상승률은 양수여야 함 (1% ~ 100%)
                    return thresholdValue.compareTo(BigDecimal.ZERO) > 0 && 
                           thresholdValue.compareTo(BigDecimal.valueOf(100)) <= 0;
                    
                case PRICE_DROP:
                case PRICE_RISE:
                    // 절대 가격은 0보다 커야 함
                    return thresholdValue.compareTo(BigDecimal.ZERO) > 0;
                    
                case PRICE_TARGET:
                    // 목표 가격은 0보다 커야 함
                    return thresholdValue.compareTo(BigDecimal.ZERO) > 0;
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("알림 조건 유효성 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 현재 ETF 가격 조회 (기준 가격 설정용)
     */
    private BigDecimal getCurrentEtfPrice(String isinCd) {
        try {
            Optional<EtfSummaryDto> etfOpt = etfDataService.getEtfDetails(isinCd);
            if (etfOpt.isPresent()) {
                return etfOpt.get().getClosePrice();
            }
            return null;
        } catch (Exception e) {
            log.warn("ETF 현재 가격 조회 실패 - ISIN: {}", isinCd, e);
            return null;
        }
    }

    /**
     * 알림 조건 통계 내부 클래스
     */
    public static class AlertConditionStatistics {
        private final long totalConditions;
        private final long activeConditions;
        private final long percentageDropCount;
        private final long percentageRiseCount;
        private final long priceTargetCount;

        public AlertConditionStatistics(long totalConditions, long activeConditions,
                                      long percentageDropCount, long percentageRiseCount, long priceTargetCount) {
            this.totalConditions = totalConditions;
            this.activeConditions = activeConditions;
            this.percentageDropCount = percentageDropCount;
            this.percentageRiseCount = percentageRiseCount;
            this.priceTargetCount = priceTargetCount;
        }

        // Getters
        public long getTotalConditions() { return totalConditions; }
        public long getActiveConditions() { return activeConditions; }
        public long getPercentageDropCount() { return percentageDropCount; }
        public long getPercentageRiseCount() { return percentageRiseCount; }
        public long getPriceTargetCount() { return priceTargetCount; }

        @Override
        public String toString() {
            return String.format("전체: %d개, 활성: %d개 (하락: %d, 상승: %d, 목표: %d)",
                totalConditions, activeConditions, percentageDropCount, percentageRiseCount, priceTargetCount);
        }
    }
}
