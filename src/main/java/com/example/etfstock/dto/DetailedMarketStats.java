package com.example.etfstock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 상세한 시장 통계 정보 DTO
 * 등락률 구간별 분포를 포함한 확장된 시장 통계
 */
@Data
@AllArgsConstructor
public class DetailedMarketStats {
    
    /**
     * 기준일자
     */
    private LocalDate baseDate;
    
    /**
     * 전체 ETF 수
     */
    private long totalCount;
    
    /**
     * 상승 ETF 수
     */
    private long risingCount;
    
    /**
     * 하락 ETF 수
     */
    private long fallingCount;
    
    /**
     * 보합 ETF 수
     */
    private long stableCount;
    
    /**
     * 등락률 구간별 분포
     * 키: 구간 레이블 ("-10", "-5", "-3", "-1", "0", "1", "3", "5", "10", "10+")
     * 값: 해당 구간의 ETF 수
     */
    private Map<String, Integer> changeRateDistribution;
    
    /**
     * 기본 생성자 - 빈 통계 생성
     */
    public DetailedMarketStats() {
        this.baseDate = LocalDate.now();
        this.totalCount = 0;
        this.risingCount = 0;
        this.fallingCount = 0;
        this.stableCount = 0;
        this.changeRateDistribution = new HashMap<>();
        
        // 기본 구간 초기화
        this.changeRateDistribution.put("-10", 0);
        this.changeRateDistribution.put("-5", 0);
        this.changeRateDistribution.put("-3", 0);
        this.changeRateDistribution.put("-1", 0);
        this.changeRateDistribution.put("0", 0);
        this.changeRateDistribution.put("1", 0);
        this.changeRateDistribution.put("3", 0);
        this.changeRateDistribution.put("5", 0);
        this.changeRateDistribution.put("10", 0);
        this.changeRateDistribution.put("10+", 0);
    }
}
