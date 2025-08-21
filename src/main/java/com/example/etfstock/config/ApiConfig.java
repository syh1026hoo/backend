package com.example.etfstock.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 연동을 위한 설정 클래스
 */
@Configuration
@Getter
public class ApiConfig {

    /**
     * 공공데이터포털 서비스 키
     */
    @Value("${external.api.data-go-kr.service-key}")
    private String serviceKey;

    /**
     * 공공데이터포털 기본 URL
     */
    @Value("${external.api.data-go-kr.base-url}")
    private String baseUrl;

    /**
     * ETF 정보 조회 엔드포인트
     */
    @Value("${external.api.data-go-kr.etf-endpoint}")
    private String etfEndpoint;

    /**
     * RestTemplate Bean 등록
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * ETF API 전체 URL 반환
     */
    public String getEtfApiUrl() {
        return baseUrl + etfEndpoint;
    }
}
