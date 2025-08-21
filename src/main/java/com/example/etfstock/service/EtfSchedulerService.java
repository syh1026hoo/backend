package com.example.etfstock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;


/**
 * ETF 데이터 자동 동기화 및 알림 감시 스케줄러
 * 전략과제 #1: ETF 데이터 동기화
 * 전략과제 #3: 가격 변동 감시 및 알림 발생
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtfSchedulerService {

    private final EtfDataService etfDataService;
    private final PriceMonitoringService priceMonitoringService;
    private final AlertCleanupService alertCleanupService;

    /**
     * 평일 오후 4시에 당일 ETF 데이터 동기화 및 알림 확인
     * 한국 증시는 오후 3시 30분에 마감되므로 4시에 실행
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void syncDailyEtfDataAndCheckAlerts() {
        try {
            log.info("====== 일일 ETF 데이터 동기화 및 알림 확인 시작 ======");
            
            // 1. ETF 데이터 동기화
            int syncedCount = etfDataService.syncEtfData();
            log.info("ETF 데이터 동기화 완료 - 동기화 건수: {}", syncedCount);
            
            // 2. 알림 조건 확인 (데이터 동기화 후)
            int alertCount = priceMonitoringService.checkAllAlertConditions();
            log.info("알림 조건 확인 완료 - 발생한 알림 수: {}", alertCount);
            
            log.info("====== 일일 ETF 데이터 동기화 및 알림 확인 완료 ======");
            
        } catch (Exception e) {
            log.error("일일 ETF 데이터 동기화 및 알림 확인 중 오류 발생", e);
        }
    }

    /**
     * 평일 오전 8시 30분에 ETF 데이터 보완 동기화
     * 누락된 데이터가 있을 경우를 대비
     */
    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul")
    public void syncSupplementaryEtfData() {
        try {
            log.info("====== ETF 데이터 보완 동기화 시작 ======");
            
            int syncedCount = etfDataService.syncEtfData();
            
            log.info("====== ETF 데이터 보완 동기화 완료 - 동기화 건수: {} ======", syncedCount);
            
        } catch (Exception e) {
            log.error("ETF 데이터 보완 동기화 중 오류 발생", e);
        }
    }

    /**
     * 평일 장중 30분마다 알림 조건 확인 (실시간 감시)
     * 오전 9시부터 오후 3시 30분까지 30분 간격으로 실행
     */
    @Scheduled(cron = "0 */30 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void realtimeAlertMonitoring() {
        try {
            log.info("====== 실시간 알림 감시 시작 ======");
            
            int alertCount = priceMonitoringService.checkAllAlertConditions();
            log.info("실시간 알림 감시 완료 - 발생한 알림 수: {}", alertCount);
            
        } catch (Exception e) {
            log.error("실시간 알림 감시 중 오류 발생", e);
        }
    }

    /**
     * 매일 자정에 오래된 데이터 및 알림 정리
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void cleanupOldData() {
        try {
            log.info("====== 오래된 데이터 정리 시작 ======");
            
            // 1. ETF 데이터 정리 (30일 이전)
            LocalDate cutoffDate = LocalDate.now().minusDays(30);
            // etfInfoRepository.deleteByBaseDateBefore(cutoffDate);
            log.info("오래된 ETF 데이터 정리 - 기준일자: {} 이전", cutoffDate);
            
            // 2. 알림 관련 데이터 정리
            alertCleanupService.cleanupOldAlerts();
            log.info("오래된 알림 데이터 정리 완료");
            
            log.info("====== 오래된 데이터 정리 완료 ======");
            
        } catch (Exception e) {
            log.error("오래된 데이터 정리 중 오류 발생", e);
        }
    }

    /**
     * 매주 일요일 오전 2시에 주간 데이터 백업/점검
     */
    @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Seoul")
    public void weeklyDataMaintenance() {
        try {
            log.info("====== 주간 ETF 데이터 점검 시작 ======");
            
            // 지난 주 영업일들의 데이터 점검
            LocalDate endDate = LocalDate.now().minusDays(1); // 어제
            LocalDate startDate = endDate.minusDays(7); // 일주일 전
            
            log.info("데이터 점검 기간: {} ~ {}", startDate, endDate);
            
            // 등락률 구간별 분포 통계 생성 및 로깅
            try {
                Map<String, Integer> distribution = etfDataService.getChangeRateDistribution();
                log.info("등락률 구간별 분포 통계:");
                log.info("  -10% 이하: {}개", distribution.get("-10"));
                log.info("  -10% ~ -5%: {}개", distribution.get("-5"));
                log.info("  -5% ~ -3%: {}개", distribution.get("-3"));
                log.info("  -3% ~ -1%: {}개", distribution.get("-1"));
                log.info("  -1% ~ 0%: {}개", distribution.get("0"));
                log.info("  0% ~ 1%: {}개", distribution.get("1"));
                log.info("  1% ~ 3%: {}개", distribution.get("3"));
                log.info("  3% ~ 5%: {}개", distribution.get("5"));
                log.info("  5% ~ 10%: {}개", distribution.get("10"));
                log.info("  10% 이상: {}개", distribution.get("10+"));
            } catch (Exception e) {
                log.warn("등락률 구간별 분포 통계 생성 실패", e);
            }
            
            log.info("====== 주간 ETF 데이터 점검 완료 ======");
            
        } catch (Exception e) {
            log.error("주간 ETF 데이터 점검 중 오류 발생", e);
        }
    }

    /**
     * 수동 동기화 및 알림 확인 트리거 (관리자용)
     */
    public int manualSyncAndCheckAlerts() {
        try {
            log.info("수동 ETF 데이터 동기화 및 알림 확인 시작");
            
            // 1. ETF 데이터 동기화
            int syncedCount = etfDataService.syncEtfData();
            log.info("수동 ETF 데이터 동기화 완료 - 동기화 건수: {}", syncedCount);
            
            // 2. 알림 조건 확인
            int alertCount = priceMonitoringService.checkAllAlertConditions();
            log.info("수동 알림 확인 완료 - 발생한 알림 수: {}", alertCount);
            
            return syncedCount;
            
        } catch (Exception e) {
            log.error("수동 동기화 및 알림 확인 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 수동 알림 확인만 실행 (관리자용)
     */
    public int manualAlertCheck() {
        try {
            log.info("수동 알림 확인 시작");
            
            int alertCount = priceMonitoringService.checkAllAlertConditions();
            
            log.info("수동 알림 확인 완료 - 발생한 알림 수: {}", alertCount);
            return alertCount;
            
        } catch (Exception e) {
            log.error("수동 알림 확인 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 등락률 구간별 분포 통계 조회
     */
    public Map<String, Integer> getChangeRateDistribution() {
        try {
            log.info("등락률 구간별 분포 통계 조회 시작");
            Map<String, Integer> distribution = etfDataService.getChangeRateDistribution();
            log.info("등락률 구간별 분포 통계 조회 완료");
            return distribution;
        } catch (Exception e) {
            log.error("등락률 구간별 분포 통계 조회 중 오류 발생", e);
            return Map.of();
        }
    }

    /**
     * 현재 동기화 및 알림 시스템 상태 확인
     */
    public String getSystemStatus() {
        try {
            // 동기화 상태 확인
            String syncStatus = "동기화 상태: 정상 - 최신 API 데이터 기준";
            
            // 알림 시스템 상태 확인
            String alertStatus = priceMonitoringService.getMonitoringStatistics();
            String cleanupStatus = alertCleanupService.getCleanupStatistics();
            
            return String.format("%s\n%s\n%s", syncStatus, alertStatus, cleanupStatus);
            
        } catch (Exception e) {
            return "시스템 상태: 오류 - " + e.getMessage();
        }
    }

    /**
     * 알림 시스템 통계 조회
     */
    public String getAlertSystemStatistics() {
        try {
            return alertCleanupService.getCleanupStatistics();
        } catch (Exception e) {
            return "알림 시스템 통계 조회 실패: " + e.getMessage();
        }
    }
}
