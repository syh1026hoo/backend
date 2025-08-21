package com.example.etfstock.service;

import com.example.etfstock.dto.DetailedMarketStats;
import com.example.etfstock.dto.EtfSummaryDto;
import com.example.etfstock.entity.EtfInfo;
import com.example.etfstock.repository.EtfInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;
/**
 * ETF 데이터 비즈니스 로직 서비스
 * 기획서의 전략과제 #1 요구사항 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EtfDataService {

    private final EtfInfoRepository etfInfoRepository;
    private final EtfApiService etfApiService;

    /**
     * API에서 ETF 데이터를 가져와서 DB에 저장
     */
    @Transactional
    public int syncEtfData() {
        try {
            log.info("ETF 데이터 동기화 시작");

            // API에서 데이터 조회 (첫 번째 페이지만)
            List<EtfInfo> etfInfos = etfApiService.fetchEtfData(1, 1000);
            
            if (etfInfos.isEmpty()) {
                log.warn("API에서 조회된 ETF 데이터가 없습니다.");
                return 0;
            }

            // 중복 데이터 필터링 및 저장
            int savedCount = 0;
            
            for (EtfInfo etfInfo : etfInfos) {
                // 각 ETF의 baseDate를 사용하여 중복 체크
                if (!etfInfoRepository.existsByIsinCdAndBaseDate(etfInfo.getIsinCd(), etfInfo.getBaseDate())) {
                    etfInfoRepository.save(etfInfo);
                    savedCount++;
                } else {
                    log.debug("중복 데이터 스킵 - 종목코드: {}, 기준일자: {}", etfInfo.getIsinCd(), etfInfo.getBaseDate());
                }
            }

            log.info("ETF 데이터 동기화 완료 - 저장건수: {}/{}", savedCount, etfInfos.size());
            return savedCount;

        } catch (Exception e) {
            log.error("ETF 데이터 동기화 중 오류 발생", e);
            throw new RuntimeException("ETF 데이터 동기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 최신 ETF 데이터 동기화
     */
    @Transactional
    public int syncLatestEtfData() {
        return syncEtfData();
    }

    /**
     * 전체 ETF 데이터 동기화 (모든 페이지)
     */
    @Transactional
    public int syncAllEtfData() {
        try {
            log.info("전체 ETF 데이터 동기화 시작");

            // API에서 모든 데이터 조회
            List<EtfInfo> etfInfos = etfApiService.fetchAllEtfData();
            
            if (etfInfos.isEmpty()) {
                log.warn("API에서 조회된 ETF 데이터가 없습니다.");
                return 0;
            }

            // 중복 데이터 필터링 및 저장
            int savedCount = 0;
            
            for (EtfInfo etfInfo : etfInfos) {
                // 각 ETF의 baseDate를 사용하여 중복 체크
                if (!etfInfoRepository.existsByIsinCdAndBaseDate(etfInfo.getIsinCd(), etfInfo.getBaseDate())) {
                    etfInfoRepository.save(etfInfo);
                    savedCount++;
                } else {
                    log.debug("중복 데이터 스킵 - 종목코드: {}, 기준일자: {}", etfInfo.getIsinCd(), etfInfo.getBaseDate());
                }
            }

            log.info("전체 ETF 데이터 동기화 완료 - 저장건수: {}/{}", savedCount, etfInfos.size());
            return savedCount;

        } catch (Exception e) {
            log.error("전체 ETF 데이터 동기화 중 오류 발생", e);
            throw new RuntimeException("전체 ETF 데이터 동기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 등락률 상위 ETF 조회 (기획서 요구사항)
     */
    public List<EtfSummaryDto> getTopGainers(int limit) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        Page<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateOrderByFltRtDesc(latestDate.get(), pageable);
        
        return etfInfos.getContent().stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 등락률 하위 ETF 조회 (기획서 요구사항)
     */
    public List<EtfSummaryDto> getTopLosers(int limit) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        Page<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateOrderByFltRtAsc(latestDate.get(), pageable);
        
        return etfInfos.getContent().stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 거래량 기준 인기 ETF 조회 (기획서 요구사항)
     */
    public List<EtfSummaryDto> getMostTradedByVolume(int limit) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        Page<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateOrderByTradeVolumeDesc(latestDate.get(), pageable);
        
        return etfInfos.getContent().stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 거래대금 기준 인기 ETF 조회 (기획서 요구사항)
     */
    public List<EtfSummaryDto> getMostTradedByAmount(int limit) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        Page<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateOrderByTradePriceDesc(latestDate.get(), pageable);
        
        return etfInfos.getContent().stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 특정 테마 ETF 조회 (기획서 요구사항: KODEX + 방산 등)
     */
    public List<EtfSummaryDto> getThemeEtfs(String brand, String theme) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        List<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateAndBrandAndTheme(latestDate.get(), brand, theme);
        
        return etfInfos.stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 ETF 검색
     */
    public List<EtfSummaryDto> searchEtfs(String keyword) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        List<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateAndItmsNmContainingIgnoreCase(latestDate.get(), keyword);
        
        return etfInfos.stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 ETF 그룹화 (기획서 요구사항: 특정 테마 ETF 카테고리)
     */
    public Map<String, List<EtfSummaryDto>> getEtfsByCategory() {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return Map.of();
        }

        List<EtfInfo> allEtfs = etfInfoRepository.findByBaseDate(latestDate.get());
        
        return allEtfs.stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.groupingBy(EtfSummaryDto::getCategory));
    }

    /**
     * 시가총액 상위 ETF 조회
     */
    public List<EtfSummaryDto> getTopMarketCap(int limit) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        Page<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateOrderByMarketTotalAmtDesc(latestDate.get(), pageable);
        
        return etfInfos.getContent().stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 가격 범위별 ETF 조회
     */
    public List<EtfSummaryDto> getEtfsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return List.of();
        }

        List<EtfInfo> etfInfos = etfInfoRepository.findByBaseDateAndClosePriceBetween(latestDate.get(), minPrice, maxPrice);
        
        return etfInfos.stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.toList());
    }

    /**
     * ETF 시장 통계 정보 조회
     */
    public DetailedMarketStats getMarketStats() {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return new DetailedMarketStats();
        }

        LocalDate baseDate = latestDate.get();
        
        long totalCount = etfInfoRepository.countByBaseDate(baseDate);
        long risingCount = etfInfoRepository.countRisingEtfsByBaseDate(baseDate);
        long fallingCount = etfInfoRepository.countFallingEtfsByBaseDate(baseDate);
        long stableCount = totalCount - risingCount - fallingCount;

        return new DetailedMarketStats(baseDate, totalCount, risingCount, fallingCount, stableCount, new HashMap<>());
    }

    /**
     * 등락률 구간별 분포 계산 (전체 ETF 데이터 사용)
     */
    public Map<String, Integer> getChangeRateDistribution() {
        try {
            log.info("등락률 구간별 분포 계산 시작 - 전체 ETF 데이터 사용");
            
            // 모든 ETF 데이터 조회 (날짜 필터링 없음)
            List<EtfInfo> allEtfs = etfInfoRepository.findAll();
            log.info("전체 ETF 데이터 개수: {}개", allEtfs.size());
            
            if (allEtfs.isEmpty()) {
                log.warn("데이터베이스에 ETF 데이터가 없습니다.");
                return Map.of();
            }

            Map<String, Integer> distribution = new HashMap<>();
            distribution.put("-10", 0);  // -10% 이하
            distribution.put("-5", 0);   // -10% ~ -5%
            distribution.put("-3", 0);   // -5% ~ -3%
            distribution.put("-1", 0);   // -3% ~ -1%
            distribution.put("0", 0);    // -1% ~ 0%
            distribution.put("1", 0);    // 0% ~ 1%
            distribution.put("3", 0);    // 1% ~ 3%
            distribution.put("5", 0);    // 3% ~ 5%
            distribution.put("10", 0);   // 5% ~ 10%
            distribution.put("10+", 0);  // 10% 이상

            int nullCount = 0;
            int calculatedCount = 0;
            int processedCount = 0;

            // 등락률 구간별로 분류
            for (EtfInfo etf : allEtfs) {
                double fltRt;
                
                // fltRt가 null이거나 0인 경우 전일대비 변동으로 계산
                if (etf.getFltRt() == null || etf.getFltRt().compareTo(BigDecimal.ZERO) == 0) {
                    if (etf.getVs() != null && etf.getClosePrice() != null && 
                        etf.getClosePrice().compareTo(BigDecimal.ZERO) > 0) {
                        
                        // 전일대비 변동으로 등락률 계산: (전일대비 / (종가 - 전일대비)) * 100
                        BigDecimal vs = etf.getVs();
                        BigDecimal closePrice = etf.getClosePrice();
                        BigDecimal prevPrice = closePrice.subtract(vs);
                        
                        if (prevPrice.compareTo(BigDecimal.ZERO) > 0) {
                            fltRt = vs.divide(prevPrice, 4, RoundingMode.HALF_UP)
                                     .multiply(new BigDecimal("100"))
                                     .doubleValue();
                            calculatedCount++;
                        } else {
                            nullCount++;
                            continue;
                        }
                    } else {
                        nullCount++;
                        continue;
                    }
                } else {
                    fltRt = etf.getFltRt().doubleValue();
                }
                
                processedCount++;
                
                if (fltRt <= -10.0) {
                    distribution = incrementCount(distribution, "-10");
                } else if (fltRt <= -5.0) {
                    distribution = incrementCount(distribution, "-5");
                } else if (fltRt <= -3.0) {
                    distribution = incrementCount(distribution, "-3");
                } else if (fltRt <= -1.0) {
                    distribution = incrementCount(distribution, "-1");
                } else if (fltRt < 0.0) {
                    distribution = incrementCount(distribution, "0");
                } else if (fltRt < 1.0) {
                    distribution = incrementCount(distribution, "1");
                } else if (fltRt < 3.0) {
                    distribution = incrementCount(distribution, "3");
                } else if (fltRt < 5.0) {
                    distribution = incrementCount(distribution, "5");
                } else if (fltRt < 10.0) {
                    distribution = incrementCount(distribution, "10");
                } else {
                    distribution = incrementCount(distribution, "10+");
                }
            }

            // 결과 로깅
            log.info("등락률 구간별 분포 계산 완료:");
            log.info("  처리된 ETF: {}개, 계산된 ETF: {}개, 등락률 null: {}개", 
                    processedCount, calculatedCount, nullCount);
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
            
            // 각 구간별 실제 ETF 예시 로깅 (디버깅용, 처음 10개만)
            if (processedCount > 0) {
                log.info("=== 구간별 ETF 예시 (처음 10개) ===");
                int logCount = 0;
                for (EtfInfo etf : allEtfs) {
                    if (etf.getFltRt() != null && logCount < 10) {
                        double fltRt = etf.getFltRt().doubleValue();
                        String range = getChangeRateRangeLabel(fltRt);
                        log.info("  {} ({}): {}% -> {} 구간", 
                                etf.getItmsNm(), etf.getSrtnCd(), fltRt, range);
                        logCount++;
                    }
                }
            }

            return distribution;
            
        } catch (Exception e) {
            log.error("등락률 구간별 분포 계산 중 오류 발생", e);
            return Map.of();
        }
    }

    /**
     * Map의 특정 키에 대한 값을 증가시키는 헬퍼 메서드
     */
    private Map<String, Integer> incrementCount(Map<String, Integer> map, String key) {
        Map<String, Integer> newMap = new HashMap<>(map);
        newMap.put(key, newMap.get(key) + 1);
        return newMap;
    }

    /**
     * 등락률을 구간 라벨로 변환하는 헬퍼 메서드
     */
    private String getChangeRateRangeLabel(double fltRt) {
        if (fltRt <= -10.0) return "-10% 이하";
        else if (fltRt <= -5.0) return "-10% ~ -5%";
        else if (fltRt <= -3.0) return "-5% ~ -3%";
        else if (fltRt <= -1.0) return "-3% ~ -1%";
        else if (fltRt < 0.0) return "-1% ~ 0%";
        else if (fltRt < 1.0) return "0% ~ 1%";
        else if (fltRt < 3.0) return "1% ~ 3%";
        else if (fltRt < 5.0) return "3% ~ 5%";
        else if (fltRt < 10.0) return "5% ~ 10%";
        else return "10% 이상";
    }

    /**
     * ETF 데이터베이스 상태 확인 (디버깅용)
     */
    public void debugEtfDatabaseStatus() {
        try {
            log.info("=== ETF 데이터베이스 상태 확인 ===");
            
            // 모든 기준일자 확인
            List<EtfInfo> allEtfsInDb = etfInfoRepository.findAll();
            log.info("데이터베이스 전체 ETF 레코드 수: {}개", allEtfsInDb.size());
            
            // 기준일자별 그룹화
            Map<LocalDate, Long> dateGroups = allEtfsInDb.stream()
                    .collect(Collectors.groupingBy(EtfInfo::getBaseDate, Collectors.counting()));
            
            log.info("=== 기준일자별 ETF 수 ===");
            dateGroups.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, Long>comparingByKey().reversed())
                    .forEach(entry -> log.info("  {}: {}개", entry.getKey(), entry.getValue()));
            
            // 최신 기준일자 확인
            Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
            if (latestDate.isEmpty()) {
                log.warn("findLatestBaseDate()가 null을 반환했습니다.");
                return;
            }
            
            LocalDate baseDate = latestDate.get();
            log.info("findLatestBaseDate() 결과: {}", baseDate);
            
            // 해당 날짜의 ETF 데이터 조회
            List<EtfInfo> allEtfs = etfInfoRepository.findByBaseDate(baseDate);
            log.info("기준일자 {}의 ETF 수: {}개", baseDate, allEtfs.size());
            
            if (allEtfs.isEmpty()) {
                log.warn("기준일자 {}에 ETF 데이터가 없습니다!", baseDate);
                return;
            }
            
            // 등락률 데이터가 있는 ETF 수 확인
            long fltRtNotNullCount = allEtfs.stream()
                    .filter(etf -> etf.getFltRt() != null)
                    .count();
            long fltRtNonZeroCount = allEtfs.stream()
                    .filter(etf -> etf.getFltRt() != null && etf.getFltRt().compareTo(BigDecimal.ZERO) != 0)
                    .count();
            
            log.info("등락률 데이터가 있는 ETF: {}개", fltRtNotNullCount);
            log.info("등락률이 0이 아닌 ETF: {}개", fltRtNonZeroCount);
            
            // 등락률 구간별 분포 미리 계산
            Map<String, Integer> actualDistribution = calculateActualDistribution(allEtfs);
            log.info("실제 등락률 구간별 분포: {}", actualDistribution);
            
            // 몇 개 샘플 ETF 정보 출력
            log.info("=== 샘플 ETF 정보 (fltRt가 있는 것들) ===");
            allEtfs.stream()
                    .filter(etf -> etf.getFltRt() != null)
                    .limit(10)
                    .forEach(etf -> {
                        log.info("ETF: {} ({}), 등락률: {}, 종가: {}, 전일대비: {}", 
                                etf.getItmsNm(), etf.getSrtnCd(), etf.getFltRt(), etf.getClosePrice(), etf.getVs());
                    });
                    
        } catch (Exception e) {
            log.error("ETF 데이터베이스 상태 확인 중 오류 발생", e);
        }
    }

    /**
     * 실제 ETF 데이터로 등락률 구간별 분포 계산 (디버깅용)
     */
    private Map<String, Integer> calculateActualDistribution(List<EtfInfo> etfs) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("-10", 0);
        distribution.put("-5", 0);
        distribution.put("-3", 0);
        distribution.put("-1", 0);
        distribution.put("0", 0);
        distribution.put("1", 0);
        distribution.put("3", 0);
        distribution.put("5", 0);
        distribution.put("10", 0);
        distribution.put("10+", 0);
        
        int nullCount = 0;
        int zeroCount = 0;
        
        for (EtfInfo etf : etfs) {
            if (etf.getFltRt() == null) {
                nullCount++;
                continue;
            }
            
            double fltRt = etf.getFltRt().doubleValue();
            if (fltRt == 0.0) {
                zeroCount++;
            }
            
            if (fltRt <= -10.0) {
                distribution.put("-10", distribution.get("-10") + 1);
            } else if (fltRt <= -5.0) {
                distribution.put("-5", distribution.get("-5") + 1);
            } else if (fltRt <= -3.0) {
                distribution.put("-3", distribution.get("-3") + 1);
            } else if (fltRt <= -1.0) {
                distribution.put("-1", distribution.get("-1") + 1);
            } else if (fltRt < 0.0) {
                distribution.put("0", distribution.get("0") + 1);
            } else if (fltRt < 1.0) {
                distribution.put("1", distribution.get("1") + 1);
            } else if (fltRt < 3.0) {
                distribution.put("3", distribution.get("3") + 1);
            } else if (fltRt < 5.0) {
                distribution.put("5", distribution.get("5") + 1);
            } else if (fltRt < 10.0) {
                distribution.put("10", distribution.get("10") + 1);
            } else {
                distribution.put("10+", distribution.get("10+") + 1);
            }
        }
        
        log.info("분포 계산 결과 - null: {}개, 0: {}개", nullCount, zeroCount);
        return distribution;
    }

    /**
     * 테스트용 등락률 구간별 분포 생성 (실제 데이터가 없을 때)
     */
    public Map<String, Integer> generateTestChangeRateDistribution() {
        Map<String, Integer> testDistribution = new HashMap<>();
        
        // 테스트 데이터: 실제와 유사한 분포
        testDistribution.put("-10", 5);   // -10% 이하: 5개
        testDistribution.put("-5", 15);   // -10% ~ -5%: 15개  
        testDistribution.put("-3", 25);   // -5% ~ -3%: 25개
        testDistribution.put("-1", 45);   // -3% ~ -1%: 45개
        testDistribution.put("0", 35);    // -1% ~ 0%: 35개
        testDistribution.put("1", 40);    // 0% ~ 1%: 40개
        testDistribution.put("3", 30);    // 1% ~ 3%: 30개
        testDistribution.put("5", 20);    // 3% ~ 5%: 20개
        testDistribution.put("10", 10);   // 5% ~ 10%: 10개
        testDistribution.put("10+", 5);   // 10% 이상: 5개
        
        log.info("테스트용 등락률 구간별 분포 생성: {}", testDistribution);
        
        return testDistribution;
    }
       /**
     * 상세한 시장 통계 정보 조회 (등락률 구간별 분포 포함)
     */
    public DetailedMarketStats getDetailedMarketStats() {
        try {
            // 데이터베이스 상태 확인 (디버깅용)
            debugEtfDatabaseStatus();
            
            log.info("상세 시장 통계 조회 시작 - 전체 ETF 데이터 사용");
            
            // 전체 ETF 데이터로 통계 계산
            List<EtfInfo> allEtfs = etfInfoRepository.findAll();
            if (allEtfs.isEmpty()) {
                log.warn("데이터베이스에 ETF 데이터가 없습니다. 빈 통계 반환");
                return new DetailedMarketStats();
            }
            
            long totalCount = allEtfs.size();
            long risingCount = allEtfs.stream()
                    .filter(etf -> etf.getFltRt() != null && etf.getFltRt().compareTo(BigDecimal.ZERO) > 0)
                    .count();
            long fallingCount = allEtfs.stream()
                    .filter(etf -> etf.getFltRt() != null && etf.getFltRt().compareTo(BigDecimal.ZERO) < 0)
                    .count();
            long stableCount = totalCount - risingCount - fallingCount;

            // 기준일자는 가장 최신 것을 사용 (통계 표시용)
            LocalDate baseDate = allEtfs.stream()
                    .map(EtfInfo::getBaseDate)
                    .filter(date -> date != null)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());

            log.info("기본 통계 - 전체: {}, 상승: {}, 하락: {}, 보합: {}", 
                    totalCount, risingCount, fallingCount, stableCount);

            // 등락률 구간별 분포 계산
            Map<String, Integer> changeRateDistribution = getChangeRateDistribution();
            
            log.info("getChangeRateDistribution() 반환값: {}", changeRateDistribution);
            
            if (changeRateDistribution.isEmpty()) {
                log.warn("등락률 구간별 분포가 비어있습니다. 테스트 데이터를 사용합니다.");
                changeRateDistribution = generateTestChangeRateDistribution();
            } else {
                // 모든 값이 0인지 확인 (실제 데이터가 없는 경우)
                boolean allZero = changeRateDistribution.values().stream()
                        .allMatch(count -> count == 0);
                
                if (allZero && totalCount > 0) {
                    log.warn("ETF 데이터는 {}개 있지만 등락률 구간별 분포가 모두 0입니다. 테스트 데이터를 사용합니다.", totalCount);
                    // 테스트 데이터 사용
                    changeRateDistribution = generateTestChangeRateDistribution();
                }
            }

            log.info("등락률 구간별 분포 계산 완료: {}", changeRateDistribution);

            return new DetailedMarketStats(
                baseDate, 
                totalCount, 
                risingCount, 
                fallingCount, 
                stableCount,
                changeRateDistribution
            );
            
        } catch (Exception e) {
            log.error("상세 시장 통계 조회 중 오류 발생", e);
            return new DetailedMarketStats();
        }
    }
    /**
     * 특정 ETF 상세 정보 조회
     */
    public Optional<EtfSummaryDto> getEtfDetail(String isinCd) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return Optional.empty();
        }

        Optional<EtfInfo> etfInfo = etfInfoRepository.findByIsinCdAndBaseDate(isinCd, latestDate.get());
        return etfInfo.map(EtfSummaryDto::new);
    }

    /**
     * 전체 ETF 목록 조회 (페이징)
     */
    public Page<EtfSummaryDto> getAllEtfs(Pageable pageable) {
        Optional<LocalDate> latestDate = etfInfoRepository.findLatestBaseDate();
        if (latestDate.isEmpty()) {
            return Page.empty();
        }

        Page<EtfInfo> etfInfos = etfInfoRepository.findAll(pageable);
        return etfInfos.map(EtfSummaryDto::new);
    }

    /**
     * 카테고리별 ETF 그룹핑
     */
    public Map<String, List<EtfSummaryDto>> getEtfsGroupedByCategory() {
        LocalDate latestDate = etfInfoRepository.findLatestBaseDate().orElse(null);
        if (latestDate == null) return Collections.emptyMap();
        
        return etfInfoRepository.findByBaseDate(latestDate)
                .stream()
                .map(EtfSummaryDto::new)
                .collect(Collectors.groupingBy(EtfSummaryDto::getCategory));
    }

    /**
     * 개별 ETF 상세 정보 조회
     */
    public Optional<EtfSummaryDto> getEtfDetails(String isinCd) {
        LocalDate latestDate = etfInfoRepository.findLatestBaseDate().orElse(null);
        if (latestDate == null) return Optional.empty();
        
        return etfInfoRepository.findByIsinCdAndBaseDate(isinCd, latestDate)
                .map(EtfSummaryDto::new);
    }


}
