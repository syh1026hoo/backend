package com.example.etfstock.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ETF 정보 엔티티
 * 공공데이터 증권상품시세정보 API에서 가져온 ETF 데이터를 저장
 */
@Entity
@Table(name = "etf_info", indexes = {
    @Index(name = "idx_etf_base_date", columnList = "baseDate"),
    @Index(name = "idx_etf_code", columnList = "isinCd"),
    @Index(name = "idx_etf_short_code", columnList = "srtnCd"),
    @Index(name = "idx_etf_change_rate", columnList = "fltRt"),
    @Index(name = "idx_etf_name", columnList = "itmsNm")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtfInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 기준일자 (API: basDt)
     */
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /**
     * 단축코드 (API: srtnCd)
     */
    @Column(name = "srtn_cd", length = 10)
    private String srtnCd;

    /**
     * 종목코드 (API: isinCd)
     */
    @Column(name = "isin_cd", nullable = false, length = 12)
    private String isinCd;

    /**
     * 종목명 (API: itmsNm)
     */
    @Column(name = "itms_nm", nullable = false, length = 200)
    private String itmsNm;

    /**
     * 종가 (API: clpr)
     */
    @Column(name = "close_price", precision = 15, scale = 2)
    private BigDecimal closePrice;

    /**
     * 전일대비 (API: vs)
     */
    @Column(name = "vs", precision = 15, scale = 2)
    private BigDecimal vs;

    /**
     * 등락률 (API: fltRt)
     */
    @Column(name = "flt_rt", precision = 8, scale = 4)
    private BigDecimal fltRt;

    /**
     * 순자산가치 NAV (API: nav)
     */
    @Column(name = "nav", precision = 15, scale = 2)
    private BigDecimal nav;

    /**
     * 시가 (API: mkp)
     */
    @Column(name = "open_price", precision = 15, scale = 2)
    private BigDecimal openPrice;

    /**
     * 고가 (API: hipr)
     */
    @Column(name = "high_price", precision = 15, scale = 2)
    private BigDecimal highPrice;

    /**
     * 저가 (API: lopr)
     */
    @Column(name = "low_price", precision = 15, scale = 2)
    private BigDecimal lowPrice;

    /**
     * 거래량 (API: trqu)
     */
    @Column(name = "trade_volume")
    private Long tradeVolume;

    /**
     * 거래대금 (API: trPrc)
     */
    @Column(name = "trade_price", precision = 20, scale = 2)
    private BigDecimal tradePrice;

    /**
     * 시가총액 (API: mrktTotAmt)
     */
    @Column(name = "market_total_amt", precision = 20, scale = 2)
    private BigDecimal marketTotalAmt;

    /**
     * 순자산총액 (API: nPptTotAmt)
     */
    @Column(name = "net_asset_total_amt", precision = 20, scale = 2)
    private BigDecimal netAssetTotalAmt;

    /**
     * 상장주식수 (API: stLstgCnt)
     */
    @Column(name = "st_lstg_cnt")
    private Long stLstgCnt;

    /**
     * 기초지수명 (API: bssIdxIdxNm)
     */
    @Column(name = "base_index_name", length = 100)
    private String baseIndexName;

    /**
     * 기초지수종가 (API: bssIdxClpr)
     */
    @Column(name = "base_index_close_price", precision = 15, scale = 4)
    private BigDecimal baseIndexClosePrice;

    /**
     * 데이터 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 데이터 수정 시간
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 특정 테마 ETF 분류를 위한 메서드
     * 종목명에서 키워드를 찾아 카테고리 분류
     */
    public String getCategory() {
        if (itmsNm == null) return "기타";
        
        String name = itmsNm.toUpperCase();
        
        // 대표적인 ETF 카테고리 분류
        if (name.contains("KODEX")) return "KODEX";
        if (name.contains("TIGER")) return "TIGER";
        if (name.contains("ARIRANG")) return "ARIRANG";
        if (name.contains("KINDEX")) return "KINDEX";
        if (name.contains("SOL")) return "SOL";
        if (name.contains("ACE")) return "ACE";
        
        // 테마별 분류
        if (name.contains("반도체") || name.contains("SEMICONDUCTOR")) return "반도체";
        if (name.contains("바이오") || name.contains("BIO")) return "바이오";
        if (name.contains("배터리") || name.contains("BATTERY")) return "배터리";
        if (name.contains("자동차") || name.contains("AUTO")) return "자동차";
        if (name.contains("방산") || name.contains("DEFENSE")) return "방산";
        if (name.contains("게임") || name.contains("GAME")) return "게임";
        if (name.contains("IT") || name.contains("기술")) return "IT/기술";
        if (name.contains("부동산") || name.contains("REIT")) return "부동산";
        if (name.contains("금") || name.contains("GOLD")) return "금";
        if (name.contains("은") || name.contains("SILVER")) return "은";
        if (name.contains("원유") || name.contains("OIL")) return "원유";
        if (name.contains("200")) return "코스피200";
        if (name.contains("코스닥")) return "코스닥";
        
        return "기타";
    }

    /**
     * ETF 브랜드 추출
     */
    public String getBrand() {
        if (itmsNm == null) return "기타";
        
        String name = itmsNm.toUpperCase();
        
        if (name.startsWith("KODEX")) return "KODEX";
        if (name.startsWith("TIGER")) return "TIGER";
        if (name.startsWith("ARIRANG")) return "ARIRANG";
        if (name.startsWith("KINDEX")) return "KINDEX";
        if (name.startsWith("SOL")) return "SOL";
        if (name.startsWith("ACE")) return "ACE";
        
        return "기타";
    }

    /**
     * 등락률 기준으로 상승/하락 구분
     */
    public String getPriceDirection() {
        if (fltRt == null) return "보합";
        
        if (fltRt.compareTo(BigDecimal.ZERO) > 0) return "상승";
        else if (fltRt.compareTo(BigDecimal.ZERO) < 0) return "하락";
        else return "보합";
    }
}
