package com.example.etfstock.service;

import com.example.etfstock.config.ApiConfig;
import com.example.etfstock.dto.EtfApiResponse;
import com.example.etfstock.entity.EtfInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 공공데이터포털 ETF API 연동 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtfApiService {

    private final RestTemplate restTemplate;
    private final ApiConfig apiConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * ETF 정보를 API에서 조회
     * 
     * @param pageNo 페이지 번호 (기본값: 1)
     * @param numOfRows 한 페이지 결과 수 (기본값: 100, 최대 1000)
     * @return ETF 정보 리스트
     */
    public List<EtfInfo> fetchEtfData(Integer pageNo, Integer numOfRows) {
        try {
            log.info("ETF 데이터 조회 시작 - 페이지: {}, 건수: {}", pageNo, numOfRows);

            URI uri = buildApiUri(pageNo, numOfRows);
            log.debug("API 호출 URL: {}", uri.toString());

            ResponseEntity<EtfApiResponse> response = restTemplate.getForEntity(uri, EtfApiResponse.class);
            
            if (response.getBody() == null) {
                log.warn("API 응답이 null입니다.");
                return new ArrayList<>();
            }

            EtfApiResponse apiResponse = response.getBody();
            
            // API 응답 상태 확인
            if (!isSuccessResponse(apiResponse)) {
                log.error("API 호출 실패 - 결과코드: {}, 메시지: {}", 
                    apiResponse.getResponse().getHeader().getResultCode(),
                    apiResponse.getResponse().getHeader().getResultMsg());
                return new ArrayList<>();
            }

            List<EtfInfo> etfInfos = convertApiResponseToEntity(apiResponse);
            log.info("ETF 데이터 조회 완료 - 총 {}건", etfInfos.size());
            
            return etfInfos;

        } catch (RestClientException e) {
            log.error("API 호출 중 네트워크 오류 발생", e);
            throw new RuntimeException("ETF API 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("ETF 데이터 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("ETF 데이터 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 최신 ETF 데이터 조회 (편의 메서드)
     */
    public List<EtfInfo> fetchLatestEtfData() {
        return fetchEtfData(1, 1000);
    }

    /**
     * 모든 ETF 데이터 조회 (페이징 처리)
     */
    public List<EtfInfo> fetchAllEtfData() {
        List<EtfInfo> allEtfInfos = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 1000;
        boolean hasMoreData = true;

        while (hasMoreData) {
            try {
                List<EtfInfo> pageData = fetchEtfData(pageNo, numOfRows);
                
                if (pageData.isEmpty() || pageData.size() < numOfRows) {
                    hasMoreData = false;
                }
                
                allEtfInfos.addAll(pageData);
                pageNo++;
                
                // API 호출 간격 조절 (너무 빠른 호출 방지)
                Thread.sleep(100);
                
            } catch (Exception e) {
                log.error("페이지 {} 조회 중 오류 발생", pageNo, e);
                hasMoreData = false;
            }
        }

        log.info("전체 ETF 데이터 조회 완료 - 총 {}건", allEtfInfos.size());
        return allEtfInfos;
    }

    /**
     * API URI 생성
     */
    private URI buildApiUri(Integer pageNo, Integer numOfRows) {
        return UriComponentsBuilder.fromUriString(apiConfig.getEtfApiUrl())
                .queryParam("serviceKey", apiConfig.getServiceKey())
                .queryParam("resultType", "json")
                .queryParam("pageNo", pageNo != null ? pageNo : 1)
                .queryParam("numOfRows", numOfRows != null ? numOfRows : 100)
                .build()
                .encode()
                .toUri();
    }

    /**
     * API 응답 성공 여부 확인
     */
    private boolean isSuccessResponse(EtfApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.getResponse() == null || 
            apiResponse.getResponse().getHeader() == null) {
            return false;
        }

        String resultCode = apiResponse.getResponse().getHeader().getResultCode();
        return "00".equals(resultCode);
    }

    /**
     * API 응답을 엔티티 리스트로 변환
     */
    private List<EtfInfo> convertApiResponseToEntity(EtfApiResponse apiResponse) {
        List<EtfInfo> etfInfos = new ArrayList<>();

        if (apiResponse.getResponse().getBody() == null || 
            apiResponse.getResponse().getBody().getItems() == null ||
            apiResponse.getResponse().getBody().getItems().getItem() == null) {
            log.warn("API 응답에 ETF 데이터가 없습니다.");
            return etfInfos;
        }

        List<EtfApiResponse.EtfItem> items = apiResponse.getResponse().getBody().getItems().getItem();
        
        for (EtfApiResponse.EtfItem item : items) {
            try {
                EtfInfo etfInfo = convertItemToEntity(item);
                etfInfos.add(etfInfo);
            } catch (Exception e) {
                log.warn("ETF 데이터 변환 중 오류 발생 - 종목코드: {}, 오류: {}", 
                    item.getIsinCd(), e.getMessage());
            }
        }

        return etfInfos;
    }

    /**
     * API 응답 아이템을 엔티티로 변환
     */
    private EtfInfo convertItemToEntity(EtfApiResponse.EtfItem item) {
        EtfInfo etfInfo = new EtfInfo();

        // 기본 정보
        // basDt는 API에서 제공될 수도 있고 안 될 수도 있음. 제공되면 사용, 아니면 오늘 날짜 사용
        LocalDate baseDate = parseDate(item.getBasDt());
        if (baseDate == null) {
            baseDate = LocalDate.now(); // API에서 basDt를 제공하지 않으면 오늘 날짜 사용
        }
        etfInfo.setBaseDate(baseDate);
        etfInfo.setSrtnCd(item.getSrtnCd());
        etfInfo.setIsinCd(item.getIsinCd());
        etfInfo.setItmsNm(item.getItmsNm());

        // 가격 정보 (String을 BigDecimal로 변환)
        etfInfo.setClosePrice(parseBigDecimal(item.getClpr()));
        etfInfo.setVs(parseBigDecimal(item.getVs()));
        etfInfo.setFltRt(parseBigDecimal(item.getFltRt()));
        etfInfo.setNav(parseBigDecimal(item.getNav()));
        etfInfo.setOpenPrice(parseBigDecimal(item.getMkp()));
        etfInfo.setHighPrice(parseBigDecimal(item.getHipr()));
        etfInfo.setLowPrice(parseBigDecimal(item.getLopr()));

        // 거래 정보
        etfInfo.setTradeVolume(parseLong(item.getTrqu()));
        etfInfo.setTradePrice(parseBigDecimal(item.getTrPrc()));
        etfInfo.setMarketTotalAmt(parseBigDecimal(item.getMrktTotAmt()));
        etfInfo.setNetAssetTotalAmt(parseBigDecimal(item.getNPptTotAmt()));
        etfInfo.setStLstgCnt(parseLong(item.getStLstgCnt()));
        
        // 기초지수 정보
        etfInfo.setBaseIndexName(item.getBssIdxIdxNm());
        etfInfo.setBaseIndexClosePrice(parseBigDecimal(item.getBssIdxClpr()));

        // 시간 정보
        etfInfo.setCreatedAt(LocalDateTime.now());
        etfInfo.setUpdatedAt(LocalDateTime.now());

        return etfInfo;
    }

    /**
     * 날짜 문자열 파싱 (yyyyMMdd → LocalDate)
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }

    /**
     * BigDecimal 파싱 (숫자 문자열 → BigDecimal)
     */
    private BigDecimal parseBigDecimal(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty() || "-".equals(numberStr.trim())) {
            return null;
        }
        try {
            // 쉼표 제거 후 파싱
            String cleanNumber = numberStr.replaceAll(",", "");
            return new BigDecimal(cleanNumber);
        } catch (NumberFormatException e) {
            log.warn("BigDecimal 파싱 실패: {}", numberStr);
            return null;
        }
    }

    /**
     * Long 파싱 (숫자 문자열 → Long)
     */
    private Long parseLong(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty() || "-".equals(numberStr.trim())) {
            return null;
        }
        try {
            // 쉼표 제거 후 파싱
            String cleanNumber = numberStr.replaceAll(",", "");
            return Long.parseLong(cleanNumber);
        } catch (NumberFormatException e) {
            log.warn("Long 파싱 실패: {}", numberStr);
            return null;
        }
    }
}
