package com.example.paycheck.domain.holiday.repository;

import com.example.paycheck.domain.holiday.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /**
     * 특정 날짜가 공휴일인지 확인
     */
    boolean existsByHolidayDate(LocalDate date);

    /**
     * 특정 날짜의 공휴일 정보 조회
     */
    Optional<Holiday> findByHolidayDate(LocalDate date);

    /**
     * 특정 연도의 모든 공휴일 조회
     */
    List<Holiday> findByYearOrderByHolidayDateAsc(Integer year);

    /**
     * 특정 연월의 공휴일 조회
     */
    List<Holiday> findByYearAndMonthOrderByHolidayDateAsc(Integer year, Integer month);

    /**
     * 특정 기간의 공휴일 조회
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate ORDER BY h.holidayDate ASC")
    List<Holiday> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 특정 연도의 공휴일 개수 조회
     */
    long countByYear(Integer year);

    /**
     * 특정 연도와 타입의 공휴일 조회
     */
    List<Holiday> findByYearAndTypeOrderByHolidayDateAsc(Integer year, Holiday.HolidayType type);

    /**
     * 모든 공휴일 날짜만 조회 (캐싱용)
     */
    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.year = :year")
    List<LocalDate> findAllHolidayDatesByYear(@Param("year") Integer year);

    /**
     * 특정 연도의 공휴일 존재 여부 확인
     */
    boolean existsByYear(Integer year);

    /**
     * 특정 연도의 공휴일 삭제 (업데이트 시 사용)
     */
    void deleteByYear(Integer year);

    /**
     * 특정 날짜가 공공기관 휴일인지 확인
     * (isPublicHoliday = true인 경우만)
     */
    boolean existsByHolidayDateAndIsPublicHolidayTrue(LocalDate date);

    /**
     * 특정 연도의 공공기관 휴일 날짜만 조회
     */
    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.year = :year AND h.isPublicHoliday = true")
    List<LocalDate> findAllPublicHolidayDatesByYear(@Param("year") Integer year);
}
