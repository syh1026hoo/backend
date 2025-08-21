package com.example.etfstock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ETF 요약 정보 DTO
 * 웹페이지에서 사용할 간소화된 ETF 정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtfSummaryDto {

    /**
     * 종목코드
     */
    private String isinCd;

    /**
     * 종목명
     */
    private String itmsNm;

    /**
     * 단축코드
     */
    private String srtnCd;

    /**
     * 순자산가치 NAV
     */
    private BigDecimal nav;

    /**
     * 기초지수명
     */
    private String baseIndexName;

    /**
     * 현재가
     */
    private BigDecimal closePrice;

    /**
     * 전일대비
     */
    private BigDecimal vs;

    /**
     * 등락률
     */
    private BigDecimal fltRt;

    /**
     * 거래량
     */
    private Long tradeVolume;

    /**
     * 거래대금
     */
    private BigDecimal tradePrice;

    /**
     * 시가총액
     */
    private BigDecimal marketTotalAmt;

    /**
     * 순자산총액
     */
    private BigDecimal netAssetTotalAmt;

    /**
     * 카테고리 (ETF 브랜드/테마 분류)
     */
    private String category;

    /**
     * 등락 방향 (상승/하락/보합)
     */
    private String priceDirection;

    /**
     * 기준일자
     */
    private LocalDate baseDate;

    /**
     * 생성자 - Entity에서 DTO로 변환
     */
    public EtfSummaryDto(com.example.etfstock.entity.EtfInfo etfInfo) {
        this.isinCd = etfInfo.getIsinCd();
        this.itmsNm = etfInfo.getItmsNm();
        this.srtnCd = etfInfo.getSrtnCd();
        this.nav = etfInfo.getNav();
        this.baseIndexName = etfInfo.getBaseIndexName();
        this.closePrice = etfInfo.getClosePrice();
        this.vs = etfInfo.getVs();
        this.fltRt = etfInfo.getFltRt();
        this.tradeVolume = etfInfo.getTradeVolume();
        this.tradePrice = etfInfo.getTradePrice();
        this.marketTotalAmt = etfInfo.getMarketTotalAmt();
        this.netAssetTotalAmt = etfInfo.getNetAssetTotalAmt();
        this.category = etfInfo.getCategory();
        this.priceDirection = etfInfo.getPriceDirection();
        this.baseDate = etfInfo.getBaseDate();
    }
}
