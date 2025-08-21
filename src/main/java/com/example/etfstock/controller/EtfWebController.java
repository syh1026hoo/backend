package com.example.etfstock.controller;

import com.example.etfstock.dto.EtfSummaryDto;
import com.example.etfstock.entity.Alert;
import com.example.etfstock.entity.AlertCondition;
import com.example.etfstock.service.AlertConditionService;
import com.example.etfstock.service.AlertNotificationService;
import com.example.etfstock.service.EtfDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ETF 정보 제공 플랫폼 웹 컨트롤러
 * 사용자 친화적인 웹 인터페이스 제공
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class EtfWebController {

    private final EtfDataService etfDataService;
    private final AlertConditionService alertConditionService;
    private final AlertNotificationService alertNotificationService;

    /**
     * 테스트 페이지 (임시)
     */
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    /**
     * 메인 대시보드 페이지 (간단 버전)
     * - ETF 시장 현황
     * - 상위 등락률 ETF
     * - 거래량 상위 ETF
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            log.info("메인 대시보드 페이지 요청");
            
            // 시장 통계
            // 상세한 시장 통계 (등락률 구간별 분포 포함)
            try {
                var marketStats = etfDataService.getDetailedMarketStats();
                log.info("컨트롤러에서 받은 marketStats: {}", marketStats);
                if (marketStats != null) {
                    log.info("changeRateDistribution: {}", marketStats.getChangeRateDistribution());
                    log.info("marketStats 클래스: {}", marketStats.getClass().getName());
                } else {
                    log.warn("marketStats가 null입니다!");
                }
                model.addAttribute("marketStats", marketStats);
            } catch (Exception e) {
                log.error("marketStats 조회 중 오류 발생", e);
                model.addAttribute("marketStats", new com.example.etfstock.dto.DetailedMarketStats());
            }
            
            
            // 상위 등락률 ETF (상승)
            List<EtfSummaryDto> topGainers = etfDataService.getTopGainers(5);
            model.addAttribute("topGainers", topGainers);
            
            // 하위 등락률 ETF (하락)
            List<EtfSummaryDto> topLosers = etfDataService.getTopLosers(5);
            model.addAttribute("topLosers", topLosers);
            
            // 거래량 상위 ETF
            List<EtfSummaryDto> mostTradedVolume = etfDataService.getMostTradedByVolume(5);
            model.addAttribute("mostTradedVolume", mostTradedVolume);
            
            // 거래대금 상위 ETF
            List<EtfSummaryDto> mostTradedAmount = etfDataService.getMostTradedByAmount(5);
            model.addAttribute("mostTradedAmount", mostTradedAmount);
            
            log.info("메인 대시보드 데이터 로드 완료");
            return "simple-dashboard";
            
        } catch (Exception e) {
            log.error("메인 대시보드 로딩 중 오류 발생", e);
            log.error("오류 상세:", e);
            return "test"; // 오류 시 테스트 페이지로 리다이렉트
        }
    }

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String login() {
        log.info("로그인 페이지 요청");
        return "login";
    }

    /**
     * 테스트 설정 페이지
     */
    @GetMapping("/test-setup")
    public String testSetup() {
        log.info("테스트 설정 페이지 요청");
        return "test-setup";
    }

    /**
     * 관심종목 페이지
     */
    @GetMapping("/watchlist")
    public String watchlist(Model model) {
        log.info("관심종목 페이지 요청");
        try {
            // 기본적으로는 JavaScript에서 AJAX로 데이터를 로드하므로
            // 여기서는 페이지만 렌더링
            return "watchlist";
        } catch (Exception e) {
            log.error("관심종목 페이지 로딩 중 오류 발생", e);
            return "test"; // 오류 시 테스트 페이지로 리다이렉트
        }
    }

    /**
     * ETF 검색 페이지
     */
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword, Model model) {
        try {
            log.info("ETF 검색 페이지 요청 - 키워드: {}", keyword);
            
            model.addAttribute("keyword", keyword);
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                List<EtfSummaryDto> searchResults = etfDataService.searchEtfs(keyword.trim());
                model.addAttribute("searchResults", searchResults);
                model.addAttribute("resultCount", searchResults.size());
                log.info("검색 결과: {}건", searchResults.size());
            }
            
            return "search";
            
        } catch (Exception e) {
            log.error("ETF 검색 중 오류 발생", e);
            model.addAttribute("error", "검색 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 테마별 ETF 페이지
     */
    @GetMapping("/themes")
    public String themes(Model model) {
        try {
            log.info("테마별 ETF 페이지 요청");
            
            // 카테고리별 ETF 그룹핑
            Map<String, List<EtfSummaryDto>> categoryGroups = etfDataService.getEtfsGroupedByCategory();
            model.addAttribute("categoryGroups", categoryGroups);
            
            // 주요 테마들의 개수 계산
            Map<String, Integer> themeCounts = Map.of(
                "KODEX", categoryGroups.getOrDefault("KODEX", List.of()).size(),
                "TIGER", categoryGroups.getOrDefault("TIGER", List.of()).size(),
                "반도체", categoryGroups.getOrDefault("반도체", List.of()).size(),
                "SOL", categoryGroups.getOrDefault("SOL", List.of()).size(),
                "ACE", categoryGroups.getOrDefault("ACE", List.of()).size(),
                "바이오", categoryGroups.getOrDefault("바이오", List.of()).size()
            );
            model.addAttribute("themeCounts", themeCounts);
            
            log.info("테마별 ETF 데이터 로드 완료 - 총 {}개 카테고리", categoryGroups.size());
            return "themes";
            
        } catch (Exception e) {
            log.error("테마별 ETF 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 특정 테마의 ETF 목록 페이지
     */
    @GetMapping("/themes/{theme}")
    public String themeDetail(@PathVariable String theme, Model model) {
        try {
            log.info("테마 상세 페이지 요청 - 테마: {}", theme);
            
            List<EtfSummaryDto> themeEtfs = etfDataService.searchEtfs(theme);
            model.addAttribute("theme", theme);
            model.addAttribute("themeEtfs", themeEtfs);
            model.addAttribute("etfCount", themeEtfs.size());
            
            log.info("테마 '{}' ETF 조회 완료 - {}건", theme, themeEtfs.size());
            return "theme-detail";
            
        } catch (Exception e) {
            log.error("테마 상세 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 개별 ETF 상세 페이지
     */
    @GetMapping("/etf/{isinCd}")
    public String etfDetail(@PathVariable String isinCd, Model model) {
        try {
            log.info("ETF 상세 페이지 요청 - ISIN 코드: {}", isinCd);
            
            Optional<EtfSummaryDto> etfDetail = etfDataService.getEtfDetails(isinCd);
            
            if (etfDetail.isPresent()) {
                model.addAttribute("etf", etfDetail.get());
                log.info("ETF 상세 정보 로드 완료 - {}", etfDetail.get().getItmsNm());
                return "etf-detail";
            } else {
                log.warn("ETF를 찾을 수 없음 - ISIN 코드: {}", isinCd);
                model.addAttribute("error", "해당 ETF를 찾을 수 없습니다: " + isinCd);
                return "error";
            }
            
        } catch (Exception e) {
            log.error("ETF 상세 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 랭킹 페이지
     */
    @GetMapping("/rankings")
    public String rankings(@RequestParam(defaultValue = "gainers") String type, Model model) {
        try {
            log.info("랭킹 페이지 요청 - 타입: {}", type);
            
            model.addAttribute("currentType", type);
            
            switch (type) {
                case "gainers":
                    List<EtfSummaryDto> gainers = etfDataService.getTopGainers(20);
                    model.addAttribute("etfs", gainers);
                    model.addAttribute("title", "등락률 상위 ETF");
                    break;
                case "losers":
                    List<EtfSummaryDto> losers = etfDataService.getTopLosers(20);
                    model.addAttribute("etfs", losers);
                    model.addAttribute("title", "등락률 하위 ETF");
                    break;
                case "volume":
                    List<EtfSummaryDto> volume = etfDataService.getMostTradedByVolume(20);
                    model.addAttribute("etfs", volume);
                    model.addAttribute("title", "거래량 상위 ETF");
                    break;
                case "amount":
                    List<EtfSummaryDto> amount = etfDataService.getMostTradedByAmount(20);
                    model.addAttribute("etfs", amount);
                    model.addAttribute("title", "거래대금 상위 ETF");
                    break;
                default:
                    List<EtfSummaryDto> defaultGainers = etfDataService.getTopGainers(20);
                    model.addAttribute("etfs", defaultGainers);
                    model.addAttribute("title", "등락률 상위 ETF");
                    model.addAttribute("currentType", "gainers");
            }
            
            log.info("랭킹 페이지 데이터 로드 완료 - 타입: {}", type);
            return "rankings";
            
        } catch (Exception e) {
            log.error("랭킹 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 알림 관리 페이지
     */
    @GetMapping("/alerts")
    public String alertManagement(@RequestParam(defaultValue = "1") Long userId, Model model) {
        try {
            log.info("알림 관리 페이지 요청 - userId: {}", userId);

            // 사용자의 알림 조건 목록
            List<AlertCondition> conditions = alertConditionService.getUserAlertConditions(userId);
            model.addAttribute("alertConditions", conditions);

            // 사용자의 최근 알림 목록 (상위 10개)
            List<Alert> recentAlerts = alertNotificationService.getTopNAlerts(userId, 10);
            model.addAttribute("recentAlerts", recentAlerts);

            // 읽지 않은 알림 개수
            long unreadCount = alertNotificationService.getUnreadAlertCount(userId);
            model.addAttribute("unreadCount", unreadCount);

            // 알림 조건 통계
            AlertConditionService.AlertConditionStatistics conditionStats = 
                alertConditionService.getUserConditionStatistics(userId);
            model.addAttribute("conditionStats", conditionStats);

            // 알림 통계
            AlertNotificationService.UserAlertStatistics alertStats = 
                alertNotificationService.getUserAlertStatistics(userId);
            model.addAttribute("alertStats", alertStats);

            // 조건 타입 목록 (UI에서 사용)
            model.addAttribute("conditionTypes", AlertCondition.ConditionType.values());

            model.addAttribute("currentUserId", userId);
            model.addAttribute("pageTitle", "알림 관리");

            return "alert-management";

        } catch (Exception e) {
            log.error("알림 관리 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "알림 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * 알림 상세 조회 페이지
     */
    @GetMapping("/alerts/{alertId}")
    public String alertDetail(@PathVariable Long alertId, 
                             @RequestParam(defaultValue = "1") Long userId, 
                             Model model) {
        try {
            log.info("알림 상세 페이지 요청 - alertId: {}, userId: {}", alertId, userId);

            Optional<Alert> alertOpt = alertNotificationService.getAlert(alertId, userId);
            if (alertOpt.isEmpty()) {
                model.addAttribute("error", "알림을 찾을 수 없습니다.");
                return "error";
            }

            Alert alert = alertOpt.get();
            model.addAttribute("alert", alert);
            model.addAttribute("currentUserId", userId);
            model.addAttribute("pageTitle", "알림 상세");

            // 알림을 읽음으로 자동 처리
            if (!alert.isRead()) {
                alertNotificationService.markAlertAsRead(alertId, userId);
            }

            return "alert-detail";

        } catch (Exception e) {
            log.error("알림 상세 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "알림 상세 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }

    /**
     * ETF 상세 페이지에 알림 조건 추가
     */
    @GetMapping("/etf/{isinCd}/alerts")
    public String etfAlertManagement(@PathVariable String isinCd, 
                                   @RequestParam(defaultValue = "1") Long userId, 
                                   Model model) {
        try {
            log.info("ETF 알림 관리 페이지 요청 - isinCd: {}, userId: {}", isinCd, userId);

            // ETF 정보 조회
            Optional<EtfSummaryDto> etfOpt = etfDataService.getEtfDetails(isinCd);
            if (etfOpt.isEmpty()) {
                model.addAttribute("error", "ETF 정보를 찾을 수 없습니다.");
                return "error";
            }

            EtfSummaryDto etf = etfOpt.get();
            model.addAttribute("etf", etf);

            // 해당 ETF의 알림 조건 목록
            List<AlertCondition> conditions = alertConditionService.getWatchListAlertConditions(userId, isinCd);
            model.addAttribute("alertConditions", conditions);

            // 해당 ETF의 알림 목록
            List<Alert> etfAlerts = alertNotificationService.getEtfAlerts(userId, isinCd);
            model.addAttribute("etfAlerts", etfAlerts);

            // 조건 타입 목록
            model.addAttribute("conditionTypes", AlertCondition.ConditionType.values());

            model.addAttribute("currentUserId", userId);
            model.addAttribute("pageTitle", etf.getItmsNm() + " - 알림 관리");

            return "etf-alert-management";

        } catch (Exception e) {
            log.error("ETF 알림 관리 페이지 로딩 중 오류 발생", e);
            model.addAttribute("error", "ETF 알림 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }
}
