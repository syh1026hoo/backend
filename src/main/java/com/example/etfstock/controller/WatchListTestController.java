package com.example.etfstock.controller;

import com.example.etfstock.service.UserService;
import com.example.etfstock.service.WatchListService;
import com.example.etfstock.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관심종목 기능 테스트 컨트롤러
 * 전략과제 #2: 관심종목 기능 테스트용 API
 */
@RestController
@RequestMapping("/api/test/watchlist")
@RequiredArgsConstructor
@Slf4j
public class WatchListTestController {

    private final UserService userService;
    private final WatchListService watchListService;

    /**
     * 테스트용 사용자 생성
     */
    @PostMapping("/create-test-user")
    public ResponseEntity<Map<String, Object>> createTestUser(
            @RequestParam(defaultValue = "testuser") String username,
            @RequestParam(defaultValue = "test@example.com") String email,
            @RequestParam(defaultValue = "테스트 사용자") String fullName) {
        try {
            log.info("테스트 사용자 생성 - username: {}", username);
            
            User user = userService.createUser(username, email, fullName, "password123");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 사용자 생성 완료",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName()
                )
            ));
            
        } catch (Exception e) {
            log.error("테스트 사용자 생성 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 빠른 관심종목 추가 테스트
     */
    @PostMapping("/quick-add")
    public ResponseEntity<Map<String, Object>> quickAddWatchList(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "KR7069500007") String isinCd,
            @RequestParam(defaultValue = "테스트 메모") String memo) {
        try {
            log.info("빠른 관심종목 추가 테스트 - userId: {}, isinCd: {}", userId, isinCd);
            
            var watchList = watchListService.addToWatchList(userId, isinCd, memo);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "관심종목 추가 완료",
                "watchList", Map.of(
                    "id", watchList.getId(),
                    "etfName", watchList.getEtfName(),
                    "memo", watchList.getMemo()
                )
            ));
            
        } catch (Exception e) {
            log.error("빠른 관심종목 추가 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 기능 전체 테스트
     */
    @PostMapping("/full-test")
    public ResponseEntity<Map<String, Object>> fullTest(@RequestParam Long userId) {
        try {
            log.info("관심종목 기능 전체 테스트 시작 - userId: {}", userId);
            
            StringBuilder testLog = new StringBuilder();
            
            // 1. KODEX 200 추가
            String kodex200 = "KR7069500007";
            watchListService.addToWatchList(userId, kodex200, "KODEX 200 테스트");
            testLog.append("✅ KODEX 200 추가 완료\n");
            
            // 2. 삼성전자 ETF 추가 (존재한다면)
            String samsungEtf = "KR7455030000";
            try {
                watchListService.addToWatchList(userId, samsungEtf, "삼성전자 테스트");
                testLog.append("✅ 삼성전자 ETF 추가 완료\n");
            } catch (Exception e) {
                testLog.append("⚠️ 삼성전자 ETF 추가 실패 (종목 없음): ").append(e.getMessage()).append("\n");
            }
            
            // 3. 관심종목 목록 조회
            var watchList = watchListService.getUserWatchList(userId);
            testLog.append("✅ 관심종목 목록 조회 완료 (").append(watchList.size()).append("개)\n");
            
            // 4. 좋아요 수 조회
            Long likeCount = watchListService.getEtfLikeCount(kodex200);
            testLog.append("✅ KODEX 200 좋아요 수: ").append(likeCount).append("\n");
            
            // 5. 인기 ETF 조회
            var popularEtfs = watchListService.getPopularEtfs(5);
            testLog.append("✅ 인기 ETF 조회 완료 (").append(popularEtfs.size()).append("개)\n");
            
            // 6. 통계 조회
            var stats = watchListService.getWatchListStatistics();
            testLog.append("✅ 통계 조회 완료 - 사용자: ").append(stats.getTotalUsers())
                   .append(", ETF: ").append(stats.getTotalEtfs())
                   .append(", 관심종목: ").append(stats.getTotalWatchLists()).append("\n");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "전체 테스트 완료",
                "testLog", testLog.toString(),
                "watchListCount", watchList.size(),
                "popularEtfCount", popularEtfs.size(),
                "statistics", Map.of(
                    "totalUsers", stats.getTotalUsers(),
                    "totalEtfs", stats.getTotalEtfs(),
                    "totalWatchLists", stats.getTotalWatchLists()
                )
            ));
            
        } catch (Exception e) {
            log.error("전체 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "테스트 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 목록과 ETF 정보 함께 조회 테스트
     */
    @GetMapping("/with-etf-info")
    public ResponseEntity<Map<String, Object>> getWatchListWithEtfInfo(@RequestParam Long userId) {
        try {
            log.info("관심종목 + ETF 정보 조회 테스트 - userId: {}", userId);
            
            var watchListWithInfo = watchListService.getUserWatchListWithEtfInfo(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "관심종목 + ETF 정보 조회 완료",
                "data", watchListWithInfo,
                "count", watchListWithInfo.size()
            ));
            
        } catch (Exception e) {
            log.error("관심종목 + ETF 정보 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 인기 ETF 순위 테스트
     */
    @GetMapping("/popular-ranking")
    public ResponseEntity<Map<String, Object>> getPopularRanking(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("인기 ETF 순위 조회 - limit: {}", limit);
            
            var popularEtfs = watchListService.getPopularEtfs(limit);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "인기 ETF 순위 조회 완료",
                "ranking", popularEtfs,
                "count", popularEtfs.size()
            ));
            
        } catch (Exception e) {
            log.error("인기 ETF 순위 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 토글 테스트
     */
    @PostMapping("/toggle-test")
    public ResponseEntity<Map<String, Object>> toggleTest(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "KR7069500007") String isinCd) {
        try {
            log.info("관심종목 토글 테스트 - userId: {}, isinCd: {}", userId, isinCd);
            
            boolean isAdded = watchListService.toggleWatchList(userId, isinCd, "토글 테스트");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isAdded", isAdded,
                "message", isAdded ? "관심종목에 추가되었습니다" : "관심종목에서 제거되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("관심종목 토글 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 초기화 (테스트 데이터 정리)
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup(@RequestParam Long userId) {
        try {
            log.info("테스트 데이터 정리 - userId: {}", userId);
            
            var watchList = watchListService.getUserWatchList(userId);
            int removedCount = 0;
            
            for (var item : watchList) {
                watchListService.removeFromWatchList(userId, item.getIsinCd());
                removedCount++;
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 데이터 정리 완료",
                "removedCount", removedCount
            ));
            
        } catch (Exception e) {
            log.error("테스트 데이터 정리 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
