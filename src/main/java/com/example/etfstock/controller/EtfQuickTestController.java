package com.example.etfstock.controller;

import com.example.etfstock.service.EtfApiService;
import com.example.etfstock.entity.EtfInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 수정된 API 구조로 실제 테스트하는 컨트롤러
 */
@RestController
@RequestMapping("/api/quick-test")
@RequiredArgsConstructor
@Slf4j
public class EtfQuickTestController {

    private final EtfApiService etfApiService;

    /**
     * 실제 공공데이터 API 호출 테스트 (소량 데이터)
     */
    @GetMapping("/api-call")
    public ResponseEntity<Map<String, Object>> testApiCall(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "5") int numOfRows) {
        
        try {
            log.info("API 호출 테스트 시작 - 페이지: {}, 건수: {}", pageNo, numOfRows);
            
            List<EtfInfo> etfInfos = etfApiService.fetchEtfData(pageNo, numOfRows);
            
            log.info("API 호출 테스트 완료 - 조회된 건수: {}", etfInfos.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API 호출 성공",
                "requestedCount", numOfRows,
                "actualCount", etfInfos.size(),
                "data", etfInfos
            ));
            
        } catch (Exception e) {
            log.error("API 호출 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "API 호출 실패: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * API 다중 페이지 데이터 확인 테스트
     */
    @GetMapping("/multi-page")
    public ResponseEntity<Map<String, Object>> testMultiPage() {
        try {
            log.info("API 다중 페이지 데이터 확인 테스트 시작");
            
            Map<String, Object> results = new java.util.HashMap<>();
            
            for (int page = 1; page <= 3; page++) {
                try {
                    List<EtfInfo> etfInfos = etfApiService.fetchEtfData(page, 10);
                    results.put("page" + page, Map.of(
                        "success", true,
                        "count", etfInfos.size(),
                        "sampleData", etfInfos.stream().limit(2).toList()
                    ));
                    log.info("페이지 {} - 조회 성공, 건수: {}", page, etfInfos.size());
                } catch (Exception e) {
                    results.put("page" + page, Map.of(
                        "success", false,
                        "error", e.getMessage()
                    ));
                    log.warn("페이지 {} - 조회 실패: {}", page, e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API 다중 페이지 데이터 확인 완료",
                "results", results
            ));
            
        } catch (Exception e) {
            log.error("API 다중 페이지 데이터 확인 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "테스트 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * ETF 데이터 구조 확인
     */
    @GetMapping("/data-structure")
    public ResponseEntity<Map<String, Object>> checkDataStructure() {
        try {
            log.info("데이터 구조 확인 테스트 시작");
            
            List<EtfInfo> etfInfos = etfApiService.fetchEtfData(1, 10);
            
            if (etfInfos.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "조회된 데이터가 없습니다."
                ));
            }
            
            // 첫 번째 ETF 데이터의 구조 확인
            EtfInfo sample = etfInfos.get(0);
            
            Map<String, Object> structure = new java.util.HashMap<>();
            structure.put("기준일자", sample.getBaseDate());
            structure.put("단축코드", sample.getSrtnCd());
            structure.put("종목코드", sample.getIsinCd());
            structure.put("종목명", sample.getItmsNm());
            structure.put("종가", sample.getClosePrice());
            structure.put("등락률", sample.getFltRt());
            structure.put("NAV", sample.getNav());
            structure.put("거래량", sample.getTradeVolume());
            structure.put("거래대금", sample.getTradePrice());
            structure.put("시가총액", sample.getMarketTotalAmt());
            structure.put("순자산총액", sample.getNetAssetTotalAmt());
            structure.put("상장주식수", sample.getStLstgCnt());
            structure.put("기초지수명", sample.getBaseIndexName());
            structure.put("기초지수종가", sample.getBaseIndexClosePrice());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "데이터 구조 확인 완료",
                "totalCount", etfInfos.size(),
                "sampleStructure", structure,
                "allSamples", etfInfos.stream().limit(3).toList()
            ));
            
        } catch (Exception e) {
            log.error("데이터 구조 확인 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "데이터 구조 확인 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * ETF 카테고리 분류 테스트
     */
    @GetMapping("/category-test")
    public ResponseEntity<Map<String, Object>> testCategoryClassification() {
        try {
            log.info("카테고리 분류 테스트 시작");
            
            List<EtfInfo> etfInfos = etfApiService.fetchEtfData(1, 50);
            
            if (etfInfos.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "조회된 데이터가 없습니다."
                ));
            }
            
            // 카테고리별 분류
            Map<String, Long> categoryCount = etfInfos.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        EtfInfo::getCategory,
                        java.util.stream.Collectors.counting()
                    ));
            
            // 브랜드별 분류
            Map<String, Long> brandCount = etfInfos.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        EtfInfo::getBrand,
                        java.util.stream.Collectors.counting()
                    ));
            
            // 샘플 데이터
            List<Map<String, Object>> samples = new java.util.ArrayList<>();
            etfInfos.stream()
                    .limit(10)
                    .forEach(etf -> {
                        Map<String, Object> sampleMap = new java.util.HashMap<>();
                        sampleMap.put("종목명", etf.getItmsNm());
                        sampleMap.put("카테고리", etf.getCategory());
                        sampleMap.put("브랜드", etf.getBrand());
                        sampleMap.put("등락률", etf.getFltRt());
                        samples.add(sampleMap);
                    });
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "카테고리 분류 테스트 완료",
                "totalCount", etfInfos.size(),
                "categoryCount", categoryCount,
                "brandCount", brandCount,
                "samples", samples
            ));
            
        } catch (Exception e) {
            log.error("카테고리 분류 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "카테고리 분류 테스트 실패: " + e.getMessage()
            ));
        }
    }
}
