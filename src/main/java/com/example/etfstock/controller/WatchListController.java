package com.example.etfstock.controller;

import com.example.etfstock.dto.WatchListDto;
import com.example.etfstock.entity.WatchList;
import com.example.etfstock.service.WatchListService;
import com.example.etfstock.service.WatchListService.EtfPopularityDto;
import com.example.etfstock.service.WatchListService.WatchListWithEtfInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관심종목 관리 API 컨트롤러
 * 전략과제 #2: 관심종목 추가/삭제/조회 및 종목별 좋아요 수 API
 */
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Slf4j
public class WatchListController {

    private final WatchListService watchListService;

    /**
     * 관심종목 추가
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addToWatchList(
            @RequestParam Long userId,
            @RequestParam String isinCd,
            @RequestParam(required = false) String memo) {
        try {
            log.info("관심종목 추가 요청 - userId: {}, isinCd: {}", userId, isinCd);
            
            WatchList watchList = watchListService.addToWatchList(userId, isinCd, memo);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "관심종목이 추가되었습니다",
                "watchList", Map.of(
                    "id", watchList.getId(),
                    "isinCd", watchList.getIsinCd(),
                    "etfName", watchList.getEtfName(),
                    "shortCode", watchList.getShortCode(),
                    "memo", watchList.getMemo() != null ? watchList.getMemo() : "",
                    "createdAt", watchList.getCreatedAt()
                )
            ));
            
        } catch (Exception e) {
            log.error("관심종목 추가 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 제거
     */
    @DeleteMapping("/{isinCd}")
    public ResponseEntity<Map<String, Object>> removeFromWatchList(
            @RequestParam Long userId,
            @PathVariable String isinCd) {
        try {
            log.info("관심종목 제거 요청 - userId: {}, isinCd: {}", userId, isinCd);
            
            watchListService.removeFromWatchList(userId, isinCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "관심종목이 제거되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("관심종목 제거 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 토글 (있으면 제거, 없으면 추가)
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleWatchList(
            @RequestParam Long userId,
            @RequestParam String isinCd,
            @RequestParam(required = false) String memo) {
        try {
            log.info("관심종목 토글 요청 - userId: {}, isinCd: {}", userId, isinCd);
            
            boolean isAdded = watchListService.toggleWatchList(userId, isinCd, memo);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isAdded", isAdded,
                "message", isAdded ? "관심종목에 추가되었습니다" : "관심종목에서 제거되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("관심종목 토글 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 사용자의 관심종목 목록 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserWatchList(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeEtfInfo) {
        try {
            log.info("사용자 관심종목 조회 - userId: {}, page: {}, size: {}", userId, page, size);
            
            if (includeEtfInfo) {
                // ETF 정보 포함 조회
                List<WatchListWithEtfInfo> watchListWithInfo = watchListService.getUserWatchListWithEtfInfo(userId);
                List<WatchListDto> dtoList = watchListWithInfo.stream()
                        .map(item -> new WatchListDto(item.getWatchList(), item.getEtfInfo()))
                        .toList();
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dtoList,
                    "totalCount", dtoList.size()
                ));
            } else {
                // 페이징 조회
                Pageable pageable = PageRequest.of(page, size);
                Page<WatchList> watchListPage = watchListService.getUserWatchList(userId, pageable);
                List<WatchListDto> dtoList = watchListPage.getContent().stream()
                        .map(WatchListDto::new)
                        .toList();
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dtoList,
                    "currentPage", page,
                    "totalPages", watchListPage.getTotalPages(),
                    "totalElements", watchListPage.getTotalElements()
                ));
            }
            
        } catch (Exception e) {
            log.error("관심종목 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<WatchList>> searchWatchList(
            @RequestParam Long userId,
            @RequestParam String keyword) {
        try {
            log.info("관심종목 검색 - userId: {}, keyword: {}", userId, keyword);
            
            List<WatchList> results = watchListService.searchUserWatchList(userId, keyword);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("관심종목 검색 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 관심종목 메모 업데이트
     */
    @PutMapping("/{isinCd}/memo")
    public ResponseEntity<Map<String, Object>> updateMemo(
            @RequestParam Long userId,
            @PathVariable String isinCd,
            @RequestParam String memo) {
        try {
            log.info("관심종목 메모 업데이트 - userId: {}, isinCd: {}", userId, isinCd);
            
            watchListService.updateWatchListMemo(userId, isinCd, memo);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "메모가 업데이트되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("메모 업데이트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 알림 설정 변경
     */
    @PutMapping("/{isinCd}/notification")
    public ResponseEntity<Map<String, Object>> updateNotification(
            @RequestParam Long userId,
            @PathVariable String isinCd,
            @RequestParam boolean enabled) {
        try {
            log.info("관심종목 알림 설정 변경 - userId: {}, isinCd: {}, enabled: {}", userId, isinCd, enabled);
            
            watchListService.updateNotificationSetting(userId, isinCd, enabled);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림 설정이 변경되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("알림 설정 변경 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 특정 ETF의 좋아요 수 조회
     */
    @GetMapping("/{isinCd}/likes")
    public ResponseEntity<Map<String, Object>> getEtfLikeCount(@PathVariable String isinCd) {
        try {
            log.info("ETF 좋아요 수 조회 - isinCd: {}", isinCd);
            
            Long likeCount = watchListService.getEtfLikeCount(isinCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isinCd", isinCd,
                "likeCount", likeCount
            ));
            
        } catch (Exception e) {
            log.error("좋아요 수 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 여러 ETF의 좋아요 수 조회
     */
    @PostMapping("/likes")
    public ResponseEntity<Map<String, Object>> getEtfLikeCounts(@RequestBody List<String> isinCds) {
        try {
            log.info("여러 ETF 좋아요 수 조회 - count: {}", isinCds.size());
            
            Map<String, Long> likeCounts = watchListService.getEtfLikeCounts(isinCds);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "likeCounts", likeCounts
            ));
            
        } catch (Exception e) {
            log.error("좋아요 수 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 인기 ETF 목록 조회 (좋아요 수 기준)
     */
    @GetMapping("/popular")
    public ResponseEntity<List<EtfPopularityDto>> getPopularEtfs(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("인기 ETF 조회 - limit: {}", limit);
            
            List<EtfPopularityDto> popularEtfs = watchListService.getPopularEtfs(limit);
            return ResponseEntity.ok(popularEtfs);
            
        } catch (Exception e) {
            log.error("인기 ETF 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 사용자의 특정 ETF 관심종목 여부 확인
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkWatchList(
            @RequestParam Long userId,
            @RequestParam String isinCd) {
        try {
            boolean isWatched = watchListService.isWatchedByUser(userId, isinCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isWatched", isWatched
            ));
            
        } catch (Exception e) {
            log.error("관심종목 여부 확인 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getWatchListStatistics() {
        try {
            log.info("관심종목 통계 조회");
            
            var statistics = watchListService.getWatchListStatistics();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "statistics", Map.of(
                    "totalUsers", statistics.getTotalUsers(),
                    "totalEtfs", statistics.getTotalEtfs(),
                    "totalWatchLists", statistics.getTotalWatchLists()
                )
            ));
            
        } catch (Exception e) {
            log.error("통계 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
