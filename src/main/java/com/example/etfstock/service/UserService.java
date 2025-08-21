package com.example.etfstock.service;

import com.example.etfstock.entity.User;
import com.example.etfstock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 관리 서비스
 * 전략과제 #2: 사용자 관리를 위한 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 생성 (회원가입)
     */
    @Transactional
    public User createUser(String username, String email, String fullName, String password) {
        log.info("새 사용자 생성 시도 - username: {}, email: {}", username, email);

        // 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + email);
        }

        // 사용자 생성
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(hashPassword(password)); // 실제로는 BCrypt 등 사용
        user.setStatus(User.UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        log.info("새 사용자 생성 완료 - ID: {}, username: {}", savedUser.getId(), savedUser.getUsername());
        
        return savedUser;
    }

    /**
     * 사용자 ID로 조회
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 사용자명으로 조회
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 이메일로 조회
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 로그인 (간단한 인증)
     */
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        log.info("사용자 인증 시도 - usernameOrEmail: {}", usernameOrEmail);
        
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // 패스워드 검증 (실제로는 BCrypt 등 사용)
            if (verifyPassword(password, user.getPasswordHash())) {
                // 마지막 로그인 시간 업데이트
                updateLastLoginTime(user.getId());
                log.info("사용자 인증 성공 - username: {}", user.getUsername());
                return Optional.of(user);
            }
        }
        
        log.warn("사용자 인증 실패 - usernameOrEmail: {}", usernameOrEmail);
        return Optional.empty();
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    @Transactional
    public void updateLastLoginTime(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * 사용자 정보 업데이트
     */
    @Transactional
    public User updateUser(Long userId, String fullName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 이메일 중복 체크 (자신 제외)
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + email);
        }

        user.setFullName(fullName);
        user.setEmail(email);

        User updatedUser = userRepository.save(user);
        log.info("사용자 정보 업데이트 완료 - ID: {}", userId);
        
        return updatedUser;
    }

    /**
     * 사용자 상태 변경
     */
    @Transactional
    public void updateUserStatus(Long userId, User.UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.setStatus(status);
        userRepository.save(user);
        
        log.info("사용자 상태 변경 완료 - ID: {}, 상태: {}", userId, status);
    }

    /**
     * 활성 사용자 목록 조회
     */
    public List<User> findActiveUsers() {
        return userRepository.findActiveUsers();
    }

    /**
     * 특정 ETF를 관심종목으로 등록한 사용자 목록
     */
    public List<User> findUsersByWatchListEtf(String isinCd) {
        return userRepository.findUsersByWatchListEtf(isinCd);
    }

    /**
     * 사용자 검색 (사용자명 또는 이름으로)
     */
    public List<User> searchUsers(String keyword) {
        List<User> usersByUsername = userRepository.findByUsernameContainingIgnoreCase(keyword);
        List<User> usersByFullName = userRepository.findByFullNameContainingIgnoreCase(keyword);
        
        // 중복 제거하여 결합
        usersByUsername.addAll(usersByFullName);
        return usersByUsername.stream().distinct().toList();
    }

    /**
     * 관심종목 통계가 포함된 사용자 목록
     */
    public List<Object[]> getUsersWithWatchListCount() {
        return userRepository.findUsersWithWatchListCount();
    }

    /**
     * 사용자 존재 여부 확인
     */
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    /**
     * 사용자명 존재 여부 확인
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 이메일 존재 여부 확인
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 패스워드 해시 (실제로는 BCrypt 등 사용)
     */
    private String hashPassword(String password) {
        // 실제 구현에서는 BCryptPasswordEncoder 등을 사용
        return "hashed_" + password;
    }

    /**
     * 패스워드 검증 (실제로는 BCrypt 등 사용)
     */
    private boolean verifyPassword(String password, String hashedPassword) {
        // 실제 구현에서는 BCryptPasswordEncoder 등을 사용
        return ("hashed_" + password).equals(hashedPassword);
    }
}
