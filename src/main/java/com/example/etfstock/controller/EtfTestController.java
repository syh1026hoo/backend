package com.example.etfstock.controller;

import com.example.etfstock.dto.DetailedMarketStats;
import com.example.etfstock.dto.EtfSummaryDto;
import com.example.etfstock.service.EtfDataService;
import com.example.etfstock.service.EtfSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ETF API 테스트용 컨트롤러
 * 개발/테스트 단계에서 API 동작 확인용
 */
@RestController
@RequestMapping("/api/test/etf")
@RequiredArgsConstructor
@Slf4j
public class EtfTestController {

    private final EtfDataService etfDataService;
    private final EtfSchedulerService etfSchedulerService;

    /**
     * 수동 ETF 데이터 동기화 테스트
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncEtfData() {
        try {
            log.info("수동 ETF 데이터 동기화 요청");
            
            int syncedCount = etfDataService.syncEtfData();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ETF 데이터 동기화 완료",
                "syncedCount", syncedCount
            ));
            
        } catch (Exception e) {
            log.error("ETF 데이터 동기화 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "ETF 데이터 동기화 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 최신 ETF 데이터 동기화 테스트 (첫 번째 페이지만)
     */
    @PostMapping("/sync/latest")
    public ResponseEntity<Map<String, Object>> syncLatestEtfData() {
        try {
            log.info("최신 ETF 데이터 동기화 요청 (첫 번째 페이지만)");
            
            int syncedCount = etfDataService.syncLatestEtfData();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "최신 ETF 데이터 동기화 완료 (첫 번째 페이지)",
                "syncedCount", syncedCount
            ));
            
        } catch (Exception e) {
            log.error("최신 ETF 데이터 동기화 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "최신 ETF 데이터 동기화 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 전체 ETF 데이터 동기화 테스트 (모든 페이지 - 주의: 시간 오래 걸림)
     */
    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, Object>> syncAllEtfData() {
        try {
            log.info("전체 ETF 데이터 동기화 요청 (모든 페이지 - 시간 오래 걸림)");
            
            int syncedCount = etfDataService.syncAllEtfData();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "전체 ETF 데이터 동기화 완료",
                "syncedCount", syncedCount
            ));
            
        } catch (Exception e) {
            log.error("전체 ETF 데이터 동기화 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "전체 ETF 데이터 동기화 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 등락률 상위 ETF 조회 테스트
     */
    @GetMapping("/top-gainers")
    public ResponseEntity<List<EtfSummaryDto>> getTopGainers(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<EtfSummaryDto> topGainers = etfDataService.getTopGainers(limit);
            return ResponseEntity.ok(topGainers);
        } catch (Exception e) {
            log.error("등락률 상위 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 등락률 하위 ETF 조회 테스트
     */
    @GetMapping("/top-losers")
    public ResponseEntity<List<EtfSummaryDto>> getTopLosers(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<EtfSummaryDto> topLosers = etfDataService.getTopLosers(limit);
            return ResponseEntity.ok(topLosers);
        } catch (Exception e) {
            log.error("등락률 하위 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 거래량 기준 인기 ETF 조회 테스트
     */
    @GetMapping("/most-traded-volume")
    public ResponseEntity<List<EtfSummaryDto>> getMostTradedByVolume(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<EtfSummaryDto> mostTraded = etfDataService.getMostTradedByVolume(limit);
            return ResponseEntity.ok(mostTraded);
        } catch (Exception e) {
            log.error("거래량 기준 인기 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 거래대금 기준 인기 ETF 조회 테스트
     */
    @GetMapping("/most-traded-amount")
    public ResponseEntity<List<EtfSummaryDto>> getMostTradedByAmount(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<EtfSummaryDto> mostTraded = etfDataService.getMostTradedByAmount(limit);
            return ResponseEntity.ok(mostTraded);
        } catch (Exception e) {
            log.error("거래대금 기준 인기 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 특정 테마 ETF 조회 테스트
     */
    @GetMapping("/theme")
    public ResponseEntity<List<EtfSummaryDto>> getThemeEtfs(
            @RequestParam String brand, 
            @RequestParam String theme) {
        try {
            List<EtfSummaryDto> themeEtfs = etfDataService.getThemeEtfs(brand, theme);
            return ResponseEntity.ok(themeEtfs);
        } catch (Exception e) {
            log.error("특정 테마 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ETF 검색 테스트
     */
    @GetMapping("/search")
    public ResponseEntity<List<EtfSummaryDto>> searchEtfs(@RequestParam String keyword) {
        try {
            List<EtfSummaryDto> searchResults = etfDataService.searchEtfs(keyword);
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            log.error("ETF 검색 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 카테고리별 ETF 조회 테스트
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<EtfSummaryDto>>> getEtfsByCategory() {
        try {
            Map<String, List<EtfSummaryDto>> categories = etfDataService.getEtfsByCategory();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("카테고리별 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ETF 시장 통계 조회 테스트
     */
    @GetMapping("/market-stats")
    public ResponseEntity<DetailedMarketStats> getMarketStats() {
        try {
            DetailedMarketStats stats = etfDataService.getMarketStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("ETF 시장 통계 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 특정 ETF 상세 정보 조회 테스트
     */
    @GetMapping("/{isinCd}")
    public ResponseEntity<EtfSummaryDto> getEtfDetail(@PathVariable String isinCd) {
        try {
            return etfDataService.getEtfDetail(isinCd)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("ETF 상세 정보 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 스케줄러 상태 확인 테스트
     */
    @GetMapping("/scheduler/status")
    public ResponseEntity<Map<String, String>> getSchedulerStatus() {
        try {
            String status = etfSchedulerService.getSystemStatus();
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            log.error("스케줄러 상태 확인 실패", e);
            return ResponseEntity.badRequest().body(Map.of("status", "오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 스케줄러 실행 테스트
     */
    @PostMapping("/scheduler/manual")
    public ResponseEntity<Map<String, Object>> manualSchedulerSync() {
        try {
            int syncedCount = etfSchedulerService.manualSyncAndCheckAlerts();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "수동 스케줄러 실행 완료",
                "syncedCount", syncedCount
            ));
        } catch (Exception e) {
            log.error("수동 스케줄러 실행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "수동 스케줄러 실행 실패: " + e.getMessage()
            ));
        }
    }
}
