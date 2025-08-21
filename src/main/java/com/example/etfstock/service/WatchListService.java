package com.example.etfstock.service;

import com.example.etfstock.dto.EtfSummaryDto;
import com.example.etfstock.entity.User;
import com.example.etfstock.entity.WatchList;
import com.example.etfstock.repository.WatchListRepository;
import com.example.etfstock.repository.UserRepository;
import com.example.etfstock.repository.EtfInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 관심종목 관리 서비스
 * 전략과제 #2: 관심종목 관리를 위한 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WatchListService {

    private final WatchListRepository watchListRepository;
    private final UserRepository userRepository;
    private final EtfInfoRepository etfInfoRepository;
    private final EtfDataService etfDataService;

    /**
     * 관심종목 추가
     */
    @Transactional
    public WatchList addToWatchList(Long userId, String isinCd, String memo) {
        log.info("관심종목 추가 시도 - userId: {}, isinCd: {}", userId, isinCd);

        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // ETF 정보 조회 (최신 정보)
        Optional<EtfSummaryDto> etfOpt = etfDataService.getEtfDetails(isinCd);
        if (etfOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 ETF입니다: " + isinCd);
        }
        EtfSummaryDto etf = etfOpt.get();

        // 기존 관심종목 확인
        Optional<WatchList> existingOpt = watchListRepository.findByUserIdAndIsinCd(userId, isinCd);
        
        if (existingOpt.isPresent()) {
            WatchList existing = existingOpt.get();
            if (existing.isActive()) {
                throw new IllegalArgumentException("이미 관심종목으로 등록되어 있습니다");
            } else {
                // 비활성화된 관심종목 재활성화
                existing.activate();
                existing.setMemo(memo);
                existing.updateEtfInfo(etf.getItmsNm(), etf.getSrtnCd());
                
                WatchList updated = watchListRepository.save(existing);
                log.info("관심종목 재활성화 완료 - ID: {}", updated.getId());
                return updated;
            }
        }

        // 새 관심종목 생성
        WatchList watchList = new WatchList(user, isinCd, etf.getItmsNm(), etf.getSrtnCd());
        watchList.setMemo(memo);
        
        WatchList saved = watchListRepository.save(watchList);
        log.info("새 관심종목 추가 완료 - ID: {}, ETF: {}", saved.getId(), etf.getItmsNm());
        
        return saved;
    }

    /**
     * 관심종목 제거
     */
    @Transactional
    public void removeFromWatchList(Long userId, String isinCd) {
        log.info("관심종목 제거 시도 - userId: {}, isinCd: {}", userId, isinCd);

        WatchList watchList = watchListRepository.findByUserIdAndIsinCdAndActiveTrue(userId, isinCd)
                .orElseThrow(() -> new IllegalArgumentException("관심종목을 찾을 수 없습니다"));

        watchList.deactivate();
        watchListRepository.save(watchList);
        
        log.info("관심종목 제거 완료 - ID: {}", watchList.getId());
    }

    /**
     * 관심종목 토글 (있으면 제거, 없으면 추가)
     */
    @Transactional
    public boolean toggleWatchList(Long userId, String isinCd, String memo) {
        boolean isWatched = watchListRepository.existsByUserIdAndIsinCdAndActiveTrue(userId, isinCd);
        
        if (isWatched) {
            removeFromWatchList(userId, isinCd);
            return false; // 제거됨
        } else {
            addToWatchList(userId, isinCd, memo);
            return true; // 추가됨
        }
    }

    /**
     * 사용자의 관심종목 목록 조회
     */
    public List<WatchList> getUserWatchList(Long userId) {
        return watchListRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * 사용자의 관심종목 목록 조회 (페이징)
     */
    public Page<WatchList> getUserWatchList(Long userId, Pageable pageable) {
        return watchListRepository.findByUserIdAndActiveTrue(userId, pageable);
    }

    /**
     * 사용자의 관심종목 ETF 정보 포함 조회
     */
    public List<WatchListWithEtfInfo> getUserWatchListWithEtfInfo(Long userId) {
        List<WatchList> watchLists = getUserWatchList(userId);
        
        return watchLists.stream()
                .map(w -> {
                    Optional<EtfSummaryDto> etfOpt = etfDataService.getEtfDetails(w.getIsinCd());
                    return new WatchListWithEtfInfo(w, etfOpt.orElse(null));
                })
                .collect(Collectors.toList());
    }

    /**
     * 관심종목 검색
     */
    public List<WatchList> searchUserWatchList(Long userId, String keyword) {
        return watchListRepository.searchUserWatchLists(userId, keyword);
    }

    /**
     * 관심종목 메모 업데이트
     */
    @Transactional
    public void updateWatchListMemo(Long userId, String isinCd, String memo) {
        WatchList watchList = watchListRepository.findByUserIdAndIsinCdAndActiveTrue(userId, isinCd)
                .orElseThrow(() -> new IllegalArgumentException("관심종목을 찾을 수 없습니다"));

        watchList.setMemo(memo);
        watchListRepository.save(watchList);
        
        log.info("관심종목 메모 업데이트 완료 - ID: {}", watchList.getId());
    }

    /**
     * 관심종목 알림 설정 변경
     */
    @Transactional
    public void updateNotificationSetting(Long userId, String isinCd, boolean enabled) {
        WatchList watchList = watchListRepository.findByUserIdAndIsinCdAndActiveTrue(userId, isinCd)
                .orElseThrow(() -> new IllegalArgumentException("관심종목을 찾을 수 없습니다"));

        watchList.setNotificationEnabled(enabled);
        watchListRepository.save(watchList);
        
        log.info("관심종목 알림 설정 변경 완료 - ID: {}, 알림: {}", watchList.getId(), enabled);
    }

    /**
     * 특정 ETF의 좋아요 수 조회
     */
    public Long getEtfLikeCount(String isinCd) {
        return watchListRepository.countByIsinCdAndActiveTrue(isinCd);
    }

    /**
     * 여러 ETF의 좋아요 수 조회
     */
    public Map<String, Long> getEtfLikeCounts(List<String> isinCds) {
        List<Object[]> results = watchListRepository.countLikesByIsinCds(isinCds);
        
        Map<String, Long> likeCounts = new HashMap<>();
        for (Object[] result : results) {
            likeCounts.put((String) result[0], (Long) result[1]);
        }
        
        // 좋아요가 없는 ETF는 0으로 설정
        for (String isinCd : isinCds) {
            likeCounts.putIfAbsent(isinCd, 0L);
        }
        
        return likeCounts;
    }

    /**
     * 인기 ETF 순위 조회 (좋아요 수 기준)
     */
    public List<EtfPopularityDto> getPopularEtfs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = watchListRepository.findTopEtfsByLikes(pageable);
        
        return results.stream()
                .map(result -> new EtfPopularityDto(
                    (String) result[0],      // isinCd
                    (String) result[1],      // etfName
                    (Long) result[2]         // likeCount
                ))
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 관심종목 개수 조회
     */
    public Long getUserWatchListCount(Long userId) {
        return watchListRepository.countByUserIdAndActiveTrue(userId);
    }

    /**
     * 사용자가 특정 ETF를 관심종목으로 등록했는지 확인
     */
    public boolean isWatchedByUser(Long userId, String isinCd) {
        return watchListRepository.existsByUserIdAndIsinCdAndActiveTrue(userId, isinCd);
    }

    /**
     * 관심종목 전체 통계
     */
    public WatchListStatistics getWatchListStatistics() {
        Object[] stats = watchListRepository.getWatchListStatistics();
        
        return new WatchListStatistics(
            (Long) stats[0],  // totalUsers
            (Long) stats[1],  // totalEtfs
            (Long) stats[2]   // totalWatchLists
        );
    }

    /**
     * 알림이 활성화된 관심종목 조회
     */
    public List<WatchList> getNotificationEnabledWatchLists(Long userId) {
        return watchListRepository.findByUserIdAndActiveTrueAndNotificationEnabledTrue(userId);
    }

    /**
     * 오래된 비활성 관심종목 정리
     */
    @Transactional
    public void cleanupOldInactiveWatchLists() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        watchListRepository.deleteInactiveWatchListsOlderThan(cutoffDate);
        log.info("오래된 비활성 관심종목 정리 완료 - 기준일: {}", cutoffDate);
    }

    /**
     * 관심종목과 ETF 정보를 함께 담는 DTO
     */
    public static class WatchListWithEtfInfo {
        private final WatchList watchList;
        private final EtfSummaryDto etfInfo;

        public WatchListWithEtfInfo(WatchList watchList, EtfSummaryDto etfInfo) {
            this.watchList = watchList;
            this.etfInfo = etfInfo;
        }

        public WatchList getWatchList() { return watchList; }
        public EtfSummaryDto getEtfInfo() { return etfInfo; }
    }

    /**
     * ETF 인기도 정보 DTO
     */
    public static class EtfPopularityDto {
        private final String isinCd;
        private final String etfName;
        private final Long likeCount;

        public EtfPopularityDto(String isinCd, String etfName, Long likeCount) {
            this.isinCd = isinCd;
            this.etfName = etfName;
            this.likeCount = likeCount;
        }

        public String getIsinCd() { return isinCd; }
        public String getEtfName() { return etfName; }
        public Long getLikeCount() { return likeCount; }
    }

    /**
     * 관심종목 통계 정보 DTO
     */
    public static class WatchListStatistics {
        private final Long totalUsers;
        private final Long totalEtfs;
        private final Long totalWatchLists;

        public WatchListStatistics(Long totalUsers, Long totalEtfs, Long totalWatchLists) {
            this.totalUsers = totalUsers;
            this.totalEtfs = totalEtfs;
            this.totalWatchLists = totalWatchLists;
        }

        public Long getTotalUsers() { return totalUsers; }
        public Long getTotalEtfs() { return totalEtfs; }
        public Long getTotalWatchLists() { return totalWatchLists; }
    }
}
