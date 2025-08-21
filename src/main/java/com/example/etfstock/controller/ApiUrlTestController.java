package com.example.etfstock.controller;

import com.example.etfstock.config.ApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * API URL 형식 테스트 컨트롤러
 */
@RestController
@RequestMapping("/api/url-test")
@RequiredArgsConstructor
@Slf4j
public class ApiUrlTestController {

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;

    /**
     * 사용자가 제공한 형식대로 API 호출 테스트 (basDt 없이)
     */
    @GetMapping("/original-format")
    public ResponseEntity<Map<String, Object>> testOriginalFormat(
            @RequestParam(defaultValue = "1") Integer numOfRows,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "xml") String resultType) {
        
        try {
            // 사용자 제공 형식: serviceKey, numOfRows, pageNo, resultType만
            URI uri = UriComponentsBuilder.fromUriString(apiConfig.getEtfApiUrl())
                    .queryParam("serviceKey", apiConfig.getServiceKey())
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("pageNo", pageNo)
                    .queryParam("resultType", resultType)
                    .build()
                    .encode()
                    .toUri();
            
            log.info("원본 형식 API 호출: {}", uri.toString());
            
            String response = restTemplate.getForObject(uri, String.class);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "원본 형식 API 호출 성공",
                "url", uri.toString(),
                "responseType", resultType,
                "responseLength", response != null ? response.length() : 0,
                "responseSample", response != null ? response.substring(0, Math.min(500, response.length())) : ""
            ));
            
        } catch (Exception e) {
            log.error("원본 형식 API 호출 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "API 호출 실패: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * 현재 구현 형식 테스트 (basDt 포함, JSON)
     */
    @GetMapping("/current-format")
    public ResponseEntity<Map<String, Object>> testCurrentFormat(
            @RequestParam String baseDate,
            @RequestParam(defaultValue = "1") Integer numOfRows,
            @RequestParam(defaultValue = "1") Integer pageNo) {
        
        try {
            // 현재 구현 형식: basDt 포함, JSON
            URI uri = UriComponentsBuilder.fromUriString(apiConfig.getEtfApiUrl())
                    .queryParam("serviceKey", apiConfig.getServiceKey())
                    .queryParam("resultType", "json")
                    .queryParam("basDt", baseDate)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("pageNo", pageNo)
                    .build()
                    .encode()
                    .toUri();
            
            log.info("현재 형식 API 호출: {}", uri.toString());
            
            String response = restTemplate.getForObject(uri, String.class);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "현재 형식 API 호출 성공",
                "url", uri.toString(),
                "baseDate", baseDate,
                "responseType", "json",
                "responseLength", response != null ? response.length() : 0,
                "responseSample", response != null ? response.substring(0, Math.min(500, response.length())) : ""
            ));
            
        } catch (Exception e) {
            log.error("현재 형식 API 호출 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "API 호출 실패: " + e.getMessage(),
                "baseDate", baseDate,
                "error", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * XML vs JSON 응답 비교 테스트
     */
    @GetMapping("/compare-formats")
    public ResponseEntity<Map<String, Object>> compareFormats(@RequestParam String baseDate) {
        
        Map<String, Object> results = new java.util.HashMap<>();
        
        // XML 형식 테스트
        try {
            URI xmlUri = UriComponentsBuilder.fromUriString(apiConfig.getEtfApiUrl())
                    .queryParam("serviceKey", apiConfig.getServiceKey())
                    .queryParam("resultType", "xml")
                    .queryParam("basDt", baseDate)
                    .queryParam("numOfRows", 3)
                    .queryParam("pageNo", 1)
                    .build()
                    .encode()
                    .toUri();
            
            String xmlResponse = restTemplate.getForObject(xmlUri, String.class);
            
            results.put("xml", Map.of(
                "success", true,
                "url", xmlUri.toString(),
                "responseLength", xmlResponse != null ? xmlResponse.length() : 0,
                "responseSample", xmlResponse != null ? xmlResponse.substring(0, Math.min(300, xmlResponse.length())) : ""
            ));
            
        } catch (Exception e) {
            results.put("xml", Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
        
        // JSON 형식 테스트
        try {
            URI jsonUri = UriComponentsBuilder.fromUriString(apiConfig.getEtfApiUrl())
                    .queryParam("serviceKey", apiConfig.getServiceKey())
                    .queryParam("resultType", "json")
                    .queryParam("basDt", baseDate)
                    .queryParam("numOfRows", 3)
                    .queryParam("pageNo", 1)
                    .build()
                    .encode()
                    .toUri();
            
            String jsonResponse = restTemplate.getForObject(jsonUri, String.class);
            
            results.put("json", Map.of(
                "success", true,
                "url", jsonUri.toString(),
                "responseLength", jsonResponse != null ? jsonResponse.length() : 0,
                "responseSample", jsonResponse != null ? jsonResponse.substring(0, Math.min(300, jsonResponse.length())) : ""
            ));
            
        } catch (Exception e) {
            results.put("json", Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "XML vs JSON 형식 비교 완료",
            "baseDate", baseDate,
            "results", results
        ));
    }

    /**
     * API URL 생성 확인
     */
    @GetMapping("/check-url")
    public ResponseEntity<Map<String, Object>> checkUrl() {
        
        String baseUrl = apiConfig.getBaseUrl();
        String endpoint = apiConfig.getEtfEndpoint();
        String fullUrl = apiConfig.getEtfApiUrl();
        String serviceKey = apiConfig.getServiceKey();
        
        // 예시 URL들 생성
        String originalFormatUrl = UriComponentsBuilder.fromUriString(fullUrl)
                .queryParam("serviceKey", "인증키")
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1)
                .queryParam("resultType", "xml")
                .build()
                .toUriString();
        
        String currentFormatUrl = UriComponentsBuilder.fromUriString(fullUrl)
                .queryParam("serviceKey", "인증키")
                .queryParam("resultType", "json")
                .queryParam("basDt", "20241220")
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1)
                .build()
                .toUriString();
        
        return ResponseEntity.ok(Map.of(
            "baseUrl", baseUrl,
            "endpoint", endpoint,
            "fullUrl", fullUrl,
            "serviceKeySet", serviceKey != null && !serviceKey.isEmpty(),
            "serviceKeyLength", serviceKey != null ? serviceKey.length() : 0,
            "originalFormatExample", originalFormatUrl,
            "currentFormatExample", currentFormatUrl,
            "userProvidedExample", "https://apis.data.go.kr/1160100/service/GetSecuritiesProductInfoService/getETFPriceInfo?serviceKey=인증키&numOfRows=1&pageNo=1&resultType=xml"
        ));
    }
}
