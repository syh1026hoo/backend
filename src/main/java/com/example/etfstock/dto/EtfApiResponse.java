package com.example.etfstock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공공데이터 ETF API 응답 DTO
 */
@Data
@NoArgsConstructor
public class EtfApiResponse {

    @JsonProperty("response")
    private Response response;

    @Data
    @NoArgsConstructor
    public static class Response {
        
        @JsonProperty("header")
        private Header header;
        
        @JsonProperty("body")
        private Body body;
    }

    @Data
    @NoArgsConstructor
    public static class Header {
        
        @JsonProperty("resultCode")
        private String resultCode;
        
        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    @Data
    @NoArgsConstructor
    public static class Body {
        
        @JsonProperty("items")
        private Items items;
        
        @JsonProperty("numOfRows")
        private Integer numOfRows;
        
        @JsonProperty("pageNo")
        private Integer pageNo;
        
        @JsonProperty("totalCount")
        private Integer totalCount;
    }

    @Data
    @NoArgsConstructor
    public static class Items {
        
        @JsonProperty("item")
        private List<EtfItem> item;
    }

    @Data
    @NoArgsConstructor
    public static class EtfItem {
        
        /**
         * 기준일자
         */
        @JsonProperty("basDt")
        private String basDt;
        
        /**
         * 단축코드
         */
        @JsonProperty("srtnCd")
        private String srtnCd;
        
        /**
         * 종목코드
         */
        @JsonProperty("isinCd")
        private String isinCd;
        
        /**
         * 종목명
         */
        @JsonProperty("itmsNm")
        private String itmsNm;
        
        /**
         * 종가
         */
        @JsonProperty("clpr")
        private String clpr;
        
        /**
         * 전일대비
         */
        @JsonProperty("vs")
        private String vs;
        
        /**
         * 등락률
         */
        @JsonProperty("fltRt")
        private String fltRt;
        
        /**
         * 순자산가치 (NAV)
         */
        @JsonProperty("nav")
        private String nav;
        
        /**
         * 시가
         */
        @JsonProperty("mkp")
        private String mkp;
        
        /**
         * 고가
         */
        @JsonProperty("hipr")
        private String hipr;
        
        /**
         * 저가
         */
        @JsonProperty("lopr")
        private String lopr;
        
        /**
         * 거래량
         */
        @JsonProperty("trqu")
        private String trqu;
        
        /**
         * 거래대금
         */
        @JsonProperty("trPrc")
        private String trPrc;
        
        /**
         * 시가총액
         */
        @JsonProperty("mrktTotAmt")
        private String mrktTotAmt;
        
        /**
         * 순자산총액
         */
        @JsonProperty("nPptTotAmt")
        private String nPptTotAmt;
        
        /**
         * 상장주식수
         */
        @JsonProperty("stLstgCnt")
        private String stLstgCnt;
        
        /**
         * 기초지수명
         */
        @JsonProperty("bssIdxIdxNm")
        private String bssIdxIdxNm;
        
        /**
         * 기초지수종가
         */
        @JsonProperty("bssIdxClpr")
        private String bssIdxClpr;
    }
}
