package com.example.etfstock.service;

import com.example.etfstock.dto.EtfSummaryDto;
import com.example.etfstock.entity.Alert;
import com.example.etfstock.entity.AlertCondition;
import com.example.etfstock.entity.WatchList;
import com.example.etfstock.repository.AlertConditionRepository;
import com.example.etfstock.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 가격 변동 감시 서비스
 * 전략과제 #3: 관심종목 가격 변동을 감시하고 알림 조건을 확인하여 알림을 발생시킴
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PriceMonitoringService {

    private final AlertConditionRepository alertConditionRepository;
    private final AlertRepository alertRepository;
    private final EtfDataService etfDataService;

    /**
     * 모든 활성 알림 조건을 확인하여 알림 발생
     */
    @Transactional
    public int checkAllAlertConditions() {
        try {
            log.info("====== 알림 조건 확인 시작 ======");

            // 감시가 필요한 모든 활성 알림 조건 조회
            List<AlertCondition> activeConditions = alertConditionRepository.findAllActiveConditionsForMonitoring();
            
            if (activeConditions.isEmpty()) {
                log.info("감시할 활성 알림 조건이 없습니다.");
                return 0;
            }

            log.info("감시할 알림 조건 수: {}", activeConditions.size());

            int triggeredCount = 0;
            for (AlertCondition condition : activeConditions) {
                try {
                    if (checkAndTriggerAlert(condition)) {
                        triggeredCount++;
                    }
                } catch (Exception e) {
                    log.error("알림 조건 확인 중 오류 발생 - 조건 ID: {}", condition.getId(), e);
                }
            }

            log.info("====== 알림 조건 확인 완료 - 발생한 알림 수: {} ======", triggeredCount);
            return triggeredCount;

        } catch (Exception e) {
            log.error("알림 조건 확인 프로세스 중 오류 발생", e);
            throw new RuntimeException("알림 조건 확인 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 개별 알림 조건 확인 및 알림 발생
     */
    @Transactional
    public boolean checkAndTriggerAlert(AlertCondition condition) {
        try {
            WatchList watchList = condition.getWatchList();
            String isinCd = watchList.getIsinCd();

            // 중복 알림 방지 - 최근 1시간 내 동일한 조건으로 알림이 발생했는지 확인
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            if (condition.getLastTriggeredAt() != null && condition.getLastTriggeredAt().isAfter(oneHourAgo)) {
                log.debug("중복 알림 방지 - 조건 ID: {}, 마지막 알림: {}", condition.getId(), condition.getLastTriggeredAt());
                return false;
            }

            // 현재 ETF 가격 정보 조회
            Optional<EtfSummaryDto> etfOpt = etfDataService.getEtfDetails(isinCd);
            if (etfOpt.isEmpty()) {
                log.warn("ETF 데이터를 찾을 수 없음 - ISIN: {}", isinCd);
                return false;
            }

            EtfSummaryDto currentEtf = etfOpt.get();
            BigDecimal currentPrice = currentEtf.getClosePrice(); // 종가
            
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("유효하지 않은 가격 데이터 - ISIN: {}, 가격: {}", isinCd, currentPrice);
                return false;
            }

            // 기준 가격 설정 (조건 생성 시점의 가격 또는 전일 종가)
            BigDecimal basePrice = condition.getBasePrice();
            if (basePrice == null) {
                // 기준 가격이 없으면 전일 종가 사용
                basePrice = currentEtf.getVs(); // 전일 대비 기준이 되는 가격
                if (basePrice == null) {
                    log.warn("기준 가격을 설정할 수 없음 - ISIN: {}", isinCd);
                    return false;
                }
                // 기준 가격 업데이트
                condition.updateBasePrice(basePrice);
                alertConditionRepository.save(condition);
            }

            // 알림 조건 확인
            boolean shouldTrigger = evaluateCondition(condition, currentPrice, basePrice);
            
            if (shouldTrigger) {
                // 알림 생성 및 저장
                Alert alert = createAlert(condition, currentPrice, basePrice, currentEtf);
                alertRepository.save(alert);

                // 알림 조건의 마지막 트리거 시점 업데이트
                condition.updateLastTriggered();
                alertConditionRepository.save(condition);

                log.info("알림 발생 - 사용자: {}, ETF: {}, 조건: {}, 현재가: {}, 기준가: {}", 
                    condition.getUser().getUsername(),
                    watchList.getEtfName(),
                    condition.getConditionType().getDescription(),
                    currentPrice,
                    basePrice);

                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("개별 알림 조건 확인 중 오류 발생 - 조건 ID: {}", condition.getId(), e);
            return false;
        }
    }

    /**
     * 알림 조건 평가
     */
    private boolean evaluateCondition(AlertCondition condition, BigDecimal currentPrice, BigDecimal basePrice) {
        AlertCondition.ConditionType conditionType = condition.getConditionType();
        BigDecimal thresholdValue = condition.getThresholdValue();

        switch (conditionType) {
            case PERCENTAGE_DROP:
                // 하락률 확인 (예: -3.0 = 3% 하락)
                BigDecimal dropPercentage = calculatePercentageChange(basePrice, currentPrice);
                return dropPercentage.compareTo(thresholdValue) <= 0; // 임계값 이하로 하락

            case PERCENTAGE_RISE:
                // 상승률 확인 (예: 5.0 = 5% 상승)
                BigDecimal risePercentage = calculatePercentageChange(basePrice, currentPrice);
                return risePercentage.compareTo(thresholdValue) >= 0; // 임계값 이상으로 상승

            case PRICE_DROP:
                // 절대 가격 하락 확인
                BigDecimal priceDrop = currentPrice.subtract(basePrice);
                return priceDrop.compareTo(thresholdValue) <= 0; // 임계값 이하로 하락

            case PRICE_RISE:
                // 절대 가격 상승 확인
                BigDecimal priceRise = currentPrice.subtract(basePrice);
                return priceRise.compareTo(thresholdValue) >= 0; // 임계값 이상으로 상승

            case PRICE_TARGET:
                // 목표 가격 도달 확인
                return currentPrice.compareTo(thresholdValue) >= 0; // 목표 가격 이상

            default:
                log.warn("지원하지 않는 알림 조건 타입: {}", conditionType);
                return false;
        }
    }

    /**
     * 변동률 계산 (퍼센트)
     */
    private BigDecimal calculatePercentageChange(BigDecimal basePrice, BigDecimal currentPrice) {
        if (basePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentPrice.subtract(basePrice)
                .divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 알림 객체 생성
     */
    private Alert createAlert(AlertCondition condition, BigDecimal currentPrice, BigDecimal basePrice, EtfSummaryDto etfData) {
        WatchList watchList = condition.getWatchList();
        
        // 변동률 및 변동금액 계산
        BigDecimal changePercentage = calculatePercentageChange(basePrice, currentPrice);
        BigDecimal changeAmount = currentPrice.subtract(basePrice);

        // 알림 타입 결정
        Alert.AlertType alertType = mapConditionTypeToAlertType(condition.getConditionType());

        // 알림 제목 및 메시지 생성
        String title = generateAlertTitle(watchList.getEtfName(), condition.getConditionType(), changePercentage);
        String message = generateAlertMessage(watchList.getEtfName(), condition.getConditionType(), 
                                            currentPrice, basePrice, changePercentage, changeAmount);

        return new Alert(
            condition,
            watchList,
            condition.getUser(),
            title,
            message,
            currentPrice,
            basePrice,
            changePercentage,
            changeAmount,
            alertType
        );
    }

    /**
     * 조건 타입을 알림 타입으로 매핑
     */
    private Alert.AlertType mapConditionTypeToAlertType(AlertCondition.ConditionType conditionType) {
        switch (conditionType) {
            case PERCENTAGE_DROP:
                return Alert.AlertType.PERCENTAGE_DROP;
            case PERCENTAGE_RISE:
                return Alert.AlertType.PERCENTAGE_RISE;
            case PRICE_DROP:
                return Alert.AlertType.PRICE_DROP;
            case PRICE_RISE:
                return Alert.AlertType.PRICE_RISE;
            case PRICE_TARGET:
                return Alert.AlertType.PRICE_TARGET;
            default:
                return Alert.AlertType.PERCENTAGE_DROP;
        }
    }

    /**
     * 알림 제목 생성
     */
    private String generateAlertTitle(String etfName, AlertCondition.ConditionType conditionType, BigDecimal changePercentage) {
        String direction = changePercentage.compareTo(BigDecimal.ZERO) >= 0 ? "상승" : "하락";
        String percentageStr = changePercentage.abs().setScale(2, RoundingMode.HALF_UP) + "%";
        
        return String.format("[%s] %s %s 알림", etfName, percentageStr, direction);
    }

    /**
     * 알림 메시지 생성
     */
    private String generateAlertMessage(String etfName, AlertCondition.ConditionType conditionType, 
                                      BigDecimal currentPrice, BigDecimal basePrice, 
                                      BigDecimal changePercentage, BigDecimal changeAmount) {
        StringBuilder message = new StringBuilder();
        
        message.append(String.format("%s의 가격이 알림 조건에 도달했습니다.\n\n", etfName));
        message.append(String.format("현재가: %,d원\n", currentPrice.intValue()));
        message.append(String.format("기준가: %,d원\n", basePrice.intValue()));
        message.append(String.format("변동금액: %s%,d원\n", 
            changeAmount.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", changeAmount.intValue()));
        message.append(String.format("변동률: %s%.2f%%\n", 
            changePercentage.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", changePercentage));
        message.append(String.format("\n알림 시간: %s", LocalDateTime.now().toString()));

        return message.toString();
    }

    /**
     * 특정 ETF에 대한 알림 조건 확인
     */
    @Transactional
    public int checkAlertConditionsForEtf(String isinCd) {
        try {
            log.info("특정 ETF 알림 조건 확인 시작 - ISIN: {}", isinCd);

            List<AlertCondition> conditions = alertConditionRepository.findActiveConditionsByIsinCd(isinCd);
            
            if (conditions.isEmpty()) {
                log.info("감시할 알림 조건이 없음 - ISIN: {}", isinCd);
                return 0;
            }

            int triggeredCount = 0;
            for (AlertCondition condition : conditions) {
                if (checkAndTriggerAlert(condition)) {
                    triggeredCount++;
                }
            }

            log.info("특정 ETF 알림 조건 확인 완료 - ISIN: {}, 발생한 알림 수: {}", isinCd, triggeredCount);
            return triggeredCount;

        } catch (Exception e) {
            log.error("특정 ETF 알림 조건 확인 중 오류 발생 - ISIN: {}", isinCd, e);
            return 0;
        }
    }

    /**
     * 알림 조건 통계
     */
    public String getMonitoringStatistics() {
        try {
            List<AlertCondition> allConditions = alertConditionRepository.findAllActiveConditionsForMonitoring();
            long totalConditions = allConditions.size();
            
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            long recentAlerts = alertRepository.findRecentAlerts(yesterday).size();
            
            return String.format("활성 알림 조건: %d개, 최근 24시간 알림: %d개", totalConditions, recentAlerts);
            
        } catch (Exception e) {
            log.error("알림 조건 통계 조회 중 오류 발생", e);
            return "통계 조회 실패: " + e.getMessage();
        }
    }
}
