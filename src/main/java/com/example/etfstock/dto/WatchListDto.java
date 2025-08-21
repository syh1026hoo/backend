package com.example.etfstock.dto;

import com.example.etfstock.entity.WatchList;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관심종목 정보 DTO
 * 전략과제 #2: 웹 인터페이스용 관심종목 데이터 전송 객체
 */
@Data
@NoArgsConstructor
public class WatchListDto {
    
    private Long id;
    private Long userId;
    private String username;
    private String isinCd;
    private String etfName;
    private String shortCode;
    private boolean active;
    private String memo;
    private boolean notificationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EtfSummaryDto etfInfo; // ETF 상세 정보

    public WatchListDto(WatchList watchList) {
        this.id = watchList.getId();
        this.userId = watchList.getUser().getId();
        this.username = watchList.getUser().getUsername();
        this.isinCd = watchList.getIsinCd();
        this.etfName = watchList.getEtfName();
        this.shortCode = watchList.getShortCode();
        this.active = watchList.isActive();
        this.memo = watchList.getMemo();
        this.notificationEnabled = watchList.isNotificationEnabled();
        this.createdAt = watchList.getCreatedAt();
        this.updatedAt = watchList.getUpdatedAt();
    }

    public WatchListDto(WatchList watchList, EtfSummaryDto etfInfo) {
        this(watchList);
        this.etfInfo = etfInfo;
    }
}
