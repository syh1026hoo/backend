package com.example.etfstock.controller;

import com.example.etfstock.entity.Alert;
import com.example.etfstock.entity.AlertCondition;
import com.example.etfstock.service.AlertConditionService;
import com.example.etfstock.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 알림 관리 API 컨트롤러
 * 전략과제 #3: 관심종목 알림 조건 설정/관리 및 알림 조회 API
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertConditionService alertConditionService;
    private final AlertNotificationService alertNotificationService;

    // ===== 알림 조건 관리 API =====

    /**
     * 알림 조건 생성
     */
    @PostMapping("/conditions")
    public ResponseEntity<Map<String, Object>> createAlertCondition(
            @RequestParam Long userId,
            @RequestParam String isinCd,
            @RequestParam String conditionType,
            @RequestParam BigDecimal thresholdValue,
            @RequestParam(required = false) String description) {
        try {
            log.info("알림 조건 생성 요청 - userId: {}, isinCd: {}, type: {}, threshold: {}", 
                userId, isinCd, conditionType, thresholdValue);

            // 조건 타입 변환
            AlertCondition.ConditionType type;
            try {
                type = AlertCondition.ConditionType.valueOf(conditionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "지원하지 않는 알림 조건 타입입니다: " + conditionType
                ));
            }

            // 유효성 검증
            if (!alertConditionService.validateCondition(type, thresholdValue)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "알림 조건값이 유효하지 않습니다"
                ));
            }

            AlertCondition condition = alertConditionService.createAlertCondition(
                userId, isinCd, type, thresholdValue, description);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림 조건이 생성되었습니다",
                "condition", buildConditionResponse(condition)
            ));

        } catch (Exception e) {
            log.error("알림 조건 생성 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 알림 조건 수정
     */
    @PutMapping("/conditions/{conditionId}")
    public ResponseEntity<Map<String, Object>> updateAlertCondition(
            @PathVariable Long conditionId,
            @RequestParam Long userId,
            @RequestParam(required = false) BigDecimal thresholdValue,
            @RequestParam(required = false) String description) {
        try {
            log.info("알림 조건 수정 요청 - conditionId: {}, userId: {}", conditionId, userId);

            AlertCondition condition = alertConditionService.updateAlertCondition(
                conditionId, userId, thresholdValue, description);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림 조건이 수정되었습니다",
                "condition", buildConditionResponse(condition)
            ));

        } catch (Exception e) {
            log.error("알림 조건 수정 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 알림 조건 삭제
     */
    @DeleteMapping("/conditions/{conditionId}")
    public ResponseEntity<Map<String, Object>> deleteAlertCondition(
            @PathVariable Long conditionId,
            @RequestParam Long userId) {
        try {
            log.info("알림 조건 삭제 요청 - conditionId: {}, userId: {}", conditionId, userId);

            alertConditionService.deleteAlertCondition(conditionId, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림 조건이 삭제되었습니다"
            ));

        } catch (Exception e) {
            log.error("알림 조건 삭제 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 사용자별 알림 조건 조회
     */
    @GetMapping("/conditions")
    public ResponseEntity<Map<String, Object>> getUserAlertConditions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("사용자 알림 조건 조회 요청 - userId: {}, page: {}, size: {}", userId, page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<AlertCondition> conditions = alertConditionService.getUserAlertConditions(userId, pageable);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "conditions", conditions.getContent().stream()
                    .map(this::buildConditionResponse)
                    .toList(),
                "pagination", Map.of(
                    "currentPage", conditions.getNumber(),
                    "totalPages", conditions.getTotalPages(),
                    "totalElements", conditions.getTotalElements(),
                    "hasNext", conditions.hasNext(),
                    "hasPrevious", conditions.hasPrevious()
                )
            ));

        } catch (Exception e) {
            log.error("사용자 알림 조건 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목별 알림 조건 조회
     */
    @GetMapping("/conditions/watchlist")
    public ResponseEntity<Map<String, Object>> getWatchListAlertConditions(
            @RequestParam Long userId,
            @RequestParam String isinCd) {
        try {
            log.info("관심종목 알림 조건 조회 요청 - userId: {}, isinCd: {}", userId, isinCd);

            List<AlertCondition> conditions = alertConditionService.getWatchListAlertConditions(userId, isinCd);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "conditions", conditions.stream()
                    .map(this::buildConditionResponse)
                    .toList()
            ));

        } catch (Exception e) {
            log.error("관심종목 알림 조건 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 알림 조건 활성화/비활성화 토글
     */
    @PatchMapping("/conditions/{conditionId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAlertCondition(
            @PathVariable Long conditionId,
            @RequestParam Long userId) {
        try {
            log.info("알림 조건 토글 요청 - conditionId: {}, userId: {}", conditionId, userId);

            boolean isActive = alertConditionService.toggleAlertCondition(conditionId, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", isActive ? "알림 조건이 활성화되었습니다" : "알림 조건이 비활성화되었습니다",
                "isActive", isActive
            ));

        } catch (Exception e) {
            log.error("알림 조건 토글 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 관심종목 알림 일괄 활성화/비활성화
     */
    @PatchMapping("/conditions/watchlist/toggle")
    public ResponseEntity<Map<String, Object>> toggleWatchListNotifications(
            @RequestParam Long userId,
            @RequestParam String isinCd,
            @RequestParam boolean enabled) {
        try {
            log.info("관심종목 알림 일괄 토글 요청 - userId: {}, isinCd: {}, enabled: {}", userId, isinCd, enabled);

            alertConditionService.toggleWatchListNotifications(userId, isinCd, enabled);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", enabled ? "관심종목 알림이 활성화되었습니다" : "관심종목 알림이 비활성화되었습니다"
            ));

        } catch (Exception e) {
            log.error("관심종목 알림 일괄 토글 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ===== 알림 조회 및 관리 API =====

    /**
     * 사용자별 알림 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserAlerts(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        try {
            log.info("사용자 알림 조회 요청 - userId: {}, page: {}, size: {}, unreadOnly: {}", 
                userId, page, size, unreadOnly);

            Pageable pageable = PageRequest.of(page, size);
            Page<Alert> alerts = unreadOnly 
                ? alertNotificationService.getUnreadAlerts(userId, pageable)
                : alertNotificationService.getUserAlerts(userId, pageable);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "alerts", alerts.getContent().stream()
                    .map(this::buildAlertResponse)
                    .toList(),
                "pagination", Map.of(
                    "currentPage", alerts.getNumber(),
                    "totalPages", alerts.getTotalPages(),
                    "totalElements", alerts.getTotalElements(),
                    "hasNext", alerts.hasNext(),
                    "hasPrevious", alerts.hasPrevious()
                )
            ));

        } catch (Exception e) {
            log.error("사용자 알림 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadAlertCount(@RequestParam Long userId) {
        try {
            long count = alertNotificationService.getUnreadAlertCount(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "unreadCount", count
            ));

        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 최근 알림 조회 (24시간 이내)
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentAlerts(@RequestParam Long userId) {
        try {
            List<Alert> alerts = alertNotificationService.getRecentAlerts(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "alerts", alerts.stream()
                    .map(this::buildAlertResponse)
                    .toList()
            ));

        } catch (Exception e) {
            log.error("최근 알림 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 고우선순위 알림 조회
     */
    @GetMapping("/high-priority")
    public ResponseEntity<Map<String, Object>> getHighPriorityAlerts(@RequestParam Long userId) {
        try {
            List<Alert> alerts = alertNotificationService.getHighPriorityAlerts(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "alerts", alerts.stream()
                    .map(this::buildAlertResponse)
                    .toList()
            ));

        } catch (Exception e) {
            log.error("고우선순위 알림 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 특정 ETF 알림 조회
     */
    @GetMapping("/etf")
    public ResponseEntity<Map<String, Object>> getEtfAlerts(
            @RequestParam Long userId,
            @RequestParam String isinCd) {
        try {
            List<Alert> alerts = alertNotificationService.getEtfAlerts(userId, isinCd);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "alerts", alerts.stream()
                    .map(this::buildAlertResponse)
                    .toList()
            ));

        } catch (Exception e) {
            log.error("ETF 알림 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{alertId}/read")
    public ResponseEntity<Map<String, Object>> markAlertAsRead(
            @PathVariable Long alertId,
            @RequestParam Long userId) {
        try {
            alertNotificationService.markAlertAsRead(alertId, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림이 읽음 처리되었습니다"
            ));

        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read/all")
    public ResponseEntity<Map<String, Object>> markAllAlertsAsRead(@RequestParam Long userId) {
        try {
            alertNotificationService.markAllAlertsAsRead(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "모든 알림이 읽음 처리되었습니다"
            ));

        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 알림 무시 처리
     */
    @PatchMapping("/{alertId}/dismiss")
    public ResponseEntity<Map<String, Object>> dismissAlert(
            @PathVariable Long alertId,
            @RequestParam Long userId) {
        try {
            alertNotificationService.dismissAlert(alertId, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림이 무시 처리되었습니다"
            ));

        } catch (Exception e) {
            log.error("알림 무시 처리 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ===== 통계 API =====

    /**
     * 사용자 알림 조건 통계
     */
    @GetMapping("/conditions/statistics")
    public ResponseEntity<Map<String, Object>> getConditionStatistics(@RequestParam Long userId) {
        try {
            AlertConditionService.AlertConditionStatistics stats = 
                alertConditionService.getUserConditionStatistics(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "statistics", Map.of(
                    "totalConditions", stats.getTotalConditions(),
                    "activeConditions", stats.getActiveConditions(),
                    "percentageDropCount", stats.getPercentageDropCount(),
                    "percentageRiseCount", stats.getPercentageRiseCount(),
                    "priceTargetCount", stats.getPriceTargetCount()
                )
            ));

        } catch (Exception e) {
            log.error("알림 조건 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 알림 통계
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics(@RequestParam Long userId) {
        try {
            AlertNotificationService.UserAlertStatistics stats = 
                alertNotificationService.getUserAlertStatistics(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "statistics", Map.of(
                    "totalAlerts", stats.getTotalAlerts(),
                    "unreadAlerts", stats.getUnreadAlerts(),
                    "recentAlerts", stats.getRecentAlerts(),
                    "highPriorityAlerts", stats.getHighPriorityAlerts(),
                    "typeStatistics", stats.getTypeStatistics()
                )
            ));

        } catch (Exception e) {
            log.error("알림 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ===== Helper Methods =====

    /**
     * 알림 조건 응답 객체 생성
     */
    private Map<String, Object> buildConditionResponse(AlertCondition condition) {
        return Map.of(
            "id", condition.getId(),
            "conditionType", condition.getConditionType().name(),
            "conditionTypeDescription", condition.getConditionType().getDescription(),
            "thresholdValue", condition.getThresholdValue(),
            "basePrice", condition.getBasePrice() != null ? condition.getBasePrice() : 0,
            "description", condition.getDescription() != null ? condition.getDescription() : "",
            "isActive", condition.isActive(),
            "createdAt", condition.getCreatedAt(),
            "lastTriggeredAt", condition.getLastTriggeredAt(),
            "watchList", Map.of(
                "id", condition.getWatchList().getId(),
                "isinCd", condition.getWatchList().getIsinCd(),
                "etfName", condition.getWatchList().getEtfName(),
                "shortCode", condition.getWatchList().getShortCode()
            )
        );
    }

    /**
     * 알림 응답 객체 생성
     */
    private Map<String, Object> buildAlertResponse(Alert alert) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", alert.getId());
        response.put("title", alert.getTitle());
        response.put("message", alert.getMessage());
        response.put("alertType", alert.getAlertType().name());
        response.put("alertTypeDescription", alert.getAlertType().getDescription());
        response.put("priority", alert.getPriority().name());
        response.put("priorityDescription", alert.getPriority().getDescription());
        response.put("triggerPrice", alert.getTriggerPrice());
        response.put("basePrice", alert.getBasePrice());
        response.put("changePercentage", alert.getChangePercentage());
        response.put("changeAmount", alert.getChangeAmount());
        response.put("isRead", alert.isRead());
        response.put("triggeredAt", alert.getTriggeredAt());
        response.put("readAt", alert.getReadAt());
        response.put("alertStatus", alert.getAlertStatus().name());
        response.put("etf", Map.of(
            "isinCd", alert.getIsinCd(),
            "etfName", alert.getEtfName()
        ));
        return response;
    }
}
