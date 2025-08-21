package com.example.etfstock.repository;

import com.example.etfstock.entity.EtfInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ETF 정보 Repository
 * 기획서의 요구사항에 맞는 데이터 조회 메서드 제공
 */
@Repository
public interface EtfInfoRepository extends JpaRepository<EtfInfo, Long> {

    /**
     * 특정 기준일자의 모든 ETF 정보 조회
     */
    List<EtfInfo> findByBaseDate(LocalDate baseDate);

    /**
     * 특정 종목코드의 ETF 정보 조회
     */
    Optional<EtfInfo> findByIsinCdAndBaseDate(String isinCd, LocalDate baseDate);

    /**
     * 단축코드로 ETF 조회
     */
    Optional<EtfInfo> findBySrtnCdAndBaseDate(String srtnCd, LocalDate baseDate);

    /**
     * 최신 기준일자 조회
     */
    @Query("SELECT MAX(e.baseDate) FROM EtfInfo e")
    Optional<LocalDate> findLatestBaseDate();

    /**
     * 등락률 순위 - 상승률 높은 순 (기획서 요구사항)
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.fltRt IS NOT NULL ORDER BY e.fltRt DESC")
    Page<EtfInfo> findByBaseDateOrderByFltRtDesc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * 등락률 순위 - 하락률 높은 순 (기획서 요구사항)  
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.fltRt IS NOT NULL ORDER BY e.fltRt ASC")
    Page<EtfInfo> findByBaseDateOrderByFltRtAsc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * 거래량 기준 인기 종목 (기획서 요구사항)
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.tradeVolume IS NOT NULL ORDER BY e.tradeVolume DESC")
    Page<EtfInfo> findByBaseDateOrderByTradeVolumeDesc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * 거래대금 기준 인기 종목 (기획서 요구사항)
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.tradePrice IS NOT NULL ORDER BY e.tradePrice DESC")
    Page<EtfInfo> findByBaseDateOrderByTradePriceDesc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * 종목명으로 검색 (특정 테마 ETF 검색용)
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND UPPER(e.itmsNm) LIKE UPPER(CONCAT('%', :keyword, '%'))")
    List<EtfInfo> findByBaseDateAndItmsNmContainingIgnoreCase(@Param("baseDate") LocalDate baseDate, @Param("keyword") String keyword);

    /**
     * 특정 테마 ETF 조회 (예: KODEX + 방산)
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND (UPPER(e.itmsNm) LIKE UPPER(CONCAT('%', :brand, '%')) AND UPPER(e.itmsNm) LIKE UPPER(CONCAT('%', :theme, '%')))")
    List<EtfInfo> findByBaseDateAndBrandAndTheme(@Param("baseDate") LocalDate baseDate, @Param("brand") String brand, @Param("theme") String theme);

    /**
     * 기초지수명으로 ETF 목록 조회
     */
    List<EtfInfo> findByBaseIndexNameContainingIgnoreCaseAndBaseDate(String baseIndexName, LocalDate baseDate);

    /**
     * 가격 범위별 조회
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.closePrice BETWEEN :minPrice AND :maxPrice")
    List<EtfInfo> findByBaseDateAndClosePriceBetween(@Param("baseDate") LocalDate baseDate, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 시가총액 상위 ETF 조회
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.marketTotalAmt IS NOT NULL ORDER BY e.marketTotalAmt DESC")
    Page<EtfInfo> findByBaseDateOrderByMarketTotalAmtDesc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * 순자산총액 상위 ETF 조회
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.netAssetTotalAmt IS NOT NULL ORDER BY e.netAssetTotalAmt DESC")
    Page<EtfInfo> findByBaseDateOrderByNetAssetTotalAmtDesc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * NAV 기준 조회
     */
    @Query("SELECT e FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.nav IS NOT NULL ORDER BY e.nav DESC")
    Page<EtfInfo> findByBaseDateOrderByNavDesc(@Param("baseDate") LocalDate baseDate, Pageable pageable);

    /**
     * 중복 데이터 체크 (동일 종목코드, 기준일자)
     */
    boolean existsByIsinCdAndBaseDate(String isinCd, LocalDate baseDate);

    /**
     * 전체 ETF 수 조회
     */
    @Query("SELECT COUNT(e) FROM EtfInfo e WHERE e.baseDate = :baseDate")
    long countByBaseDate(@Param("baseDate") LocalDate baseDate);

    /**
     * 상승 종목 수 조회
     */
    @Query("SELECT COUNT(e) FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.fltRt > 0")
    long countRisingEtfsByBaseDate(@Param("baseDate") LocalDate baseDate);

    /**
     * 하락 종목 수 조회
     */
    @Query("SELECT COUNT(e) FROM EtfInfo e WHERE e.baseDate = :baseDate AND e.fltRt < 0")
    long countFallingEtfsByBaseDate(@Param("baseDate") LocalDate baseDate);

    /**
     * 특정 기간 데이터 삭제 (스케줄링시 사용)
     */
    void deleteByBaseDateBefore(LocalDate cutoffDate);
}
