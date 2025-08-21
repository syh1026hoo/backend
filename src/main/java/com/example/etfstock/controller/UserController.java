package com.example.etfstock.controller;

import com.example.etfstock.entity.User;
import com.example.etfstock.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 사용자 관리 API 컨트롤러
 * 전략과제 #2: 사용자 인증 및 관리 API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 사용자 생성 (회원가입)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String password) {
        try {
            log.info("회원가입 요청 - username: {}, email: {}", username, email);
            
            User user = userService.createUser(username, email, fullName, password);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "회원가입이 완료되었습니다",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "status", user.getStatus().name(),
                    "createdAt", user.getCreatedAt()
                )
            ));
            
        } catch (Exception e) {
            log.error("회원가입 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String usernameOrEmail,
            @RequestParam String password) {
        try {
            log.info("로그인 요청 - usernameOrEmail: {}", usernameOrEmail);
            
            Optional<User> userOpt = userService.authenticate(usernameOrEmail, password);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "로그인 성공",
                    "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "",
                        "status", user.getStatus().name(),
                        "lastLoginAt", user.getLastLoginAt()
                    )
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "아이디 또는 비밀번호가 올바르지 않습니다"
                ));
            }
            
        } catch (Exception e) {
            log.error("로그인 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "로그인 처리 중 오류가 발생했습니다"
            ));
        }
    }

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long userId) {
        try {
            log.info("사용자 정보 조회 - userId: {}", userId);
            
            Optional<User> userOpt = userService.findById(userId);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "",
                        "status", user.getStatus().name(),
                        "createdAt", user.getCreatedAt(),
                        "lastLoginAt", user.getLastLoginAt(),
                        "watchListCount", user.getActiveWatchListCount()
                    )
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 정보 수정
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long userId,
            @RequestParam String fullName,
            @RequestParam String email) {
        try {
            log.info("사용자 정보 수정 - userId: {}", userId);
            
            User user = userService.updateUser(userId, fullName, email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "사용자 정보가 수정되었습니다",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "updatedAt", user.getUpdatedAt()
                )
            ));
            
        } catch (Exception e) {
            log.error("사용자 정보 수정 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 사용자명 중복 확인
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        try {
            boolean exists = userService.existsByUsername(username);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "exists", exists,
                "message", exists ? "이미 사용 중인 사용자명입니다" : "사용 가능한 사용자명입니다"
            ));
            
        } catch (Exception e) {
            log.error("사용자명 중복 확인 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        try {
            boolean exists = userService.existsByEmail(email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "exists", exists,
                "message", exists ? "이미 사용 중인 이메일입니다" : "사용 가능한 이메일입니다"
            ));
            
        } catch (Exception e) {
            log.error("이메일 중복 확인 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        try {
            log.info("사용자 검색 - keyword: {}", keyword);
            
            var users = userService.searchUsers(keyword);
            var userList = users.stream()
                    .map(user -> Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName() != null ? user.getFullName() : ""
                    ))
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", userList,
                "count", userList.size()
            ));
            
        } catch (Exception e) {
            log.error("사용자 검색 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 테스트용 사용자 생성 (개발 환경에서만 사용)
     */
    @PostMapping("/test-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        try {
            log.info("테스트용 사용자 생성 요청");
            
            // 테스트용 사용자 정보
            String username = "testuser";
            String email = "test@example.com";
            String fullName = "테스트 사용자";
            String password = "testpass123";
            
            // 이미 존재하는지 확인
            if (userService.existsByUsername(username)) {
                var existingUser = userService.findByUsername(username);
                if (existingUser.isPresent()) {
                    User user = existingUser.get();
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "테스트 사용자가 이미 존재합니다",
                        "user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "fullName", user.getFullName() != null ? user.getFullName() : "",
                            "status", user.getStatus().name()
                        )
                    ));
                }
            }
            
            User user = userService.createUser(username, email, fullName, password);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 사용자가 생성되었습니다",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "status", user.getStatus().name(),
                    "password", password
                )
            ));
            
        } catch (Exception e) {
            log.error("테스트용 사용자 생성 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
