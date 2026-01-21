package com.example.paycheck.domain.holiday.service;

import com.example.paycheck.domain.holiday.entity.Holiday;
import com.example.paycheck.domain.holiday.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공휴일 관리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayApiClient holidayApiClient;

    /**
     * 특정 날짜가 공공기관 휴일인지 확인 (캐싱)
     * 휴일근로 수당 적용 대상 판단에 사용
     * - 주말(토/일)
     * - 공공기관 휴일 (isPublicHoliday = true)
     *
     * @param date 확인할 날짜
     * @return 공공기관 휴일 여부
     */
    @Cacheable(value = "public-holiday-check", key = "#date")
    public boolean isPublicHoliday(LocalDate date) {
        // 1. 토요일/일요일 체크
        if (date.getDayOfWeek().getValue() >= 6) {
            return true;
        }

        // 2. DB에서 공공기관 휴일 확인 (isPublicHoliday = true인 경우만)
        return holidayRepository.existsByHolidayDateAndIsPublicHolidayTrue(date);
    }

    /**
     * 특정 연도의 모든 공휴일 날짜 조회 (캐싱)
     *
     * @param year 연도
     * @return 공휴일 날짜 Set
     */
    @Cacheable(value = "holidays-by-year", key = "#year")
    public Set<LocalDate> getHolidayDates(int year) {
        List<LocalDate> dates = holidayRepository.findAllHolidayDatesByYear(year);
        return dates.stream().collect(Collectors.toSet());
    }

    /**
     * 특정 연도의 공휴일 목록 조회
     *
     * @param year 연도
     * @return 공휴일 리스트
     */
    public List<Holiday> getHolidays(int year) {
        return holidayRepository.findByYearOrderByHolidayDateAsc(year);
    }

    /**
     * 특정 연월의 공휴일 목록 조회
     *
     * @param year 연도
     * @param month 월
     * @return 공휴일 리스트
     */
    public List<Holiday> getHolidays(int year, int month) {
        return holidayRepository.findByYearAndMonthOrderByHolidayDateAsc(year, month);
    }

    /**
     * 특정 기간의 공휴일 목록 조회
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 공휴일 리스트
     */
    public List<Holiday> getHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByDateRange(startDate, endDate);
    }

    /**
     * API에서 공휴일 정보를 가져와 DB에 저장
     *
     * @param year 연도
     * @return 저장된 공휴일 개수
     */
    @Transactional
    @CacheEvict(value = {"public-holiday-check", "holidays-by-year"}, allEntries = true)
    public int updateHolidays(int year) {
        log.info("{}년 공휴일 정보 업데이트 시작", year);

        // API 키 확인
        if (!holidayApiClient.isApiKeyConfigured()) {
            log.warn("공휴일 API 키가 설정되지 않았습니다. 업데이트를 건너뜁니다.");
            return 0;
        }

        try {
            // 1. API에서 공휴일 정보 가져오기
            List<Holiday> holidays = holidayApiClient.fetchHolidays(year);

            if (holidays.isEmpty()) {
                log.warn("{}년 공휴일 정보가 없습니다", year);
                return 0;
            }

            // 2. 기존 데이터 삭제 (해당 연도만)
            if (holidayRepository.existsByYear(year)) {
                holidayRepository.deleteByYear(year);
                log.info("{}년 기존 공휴일 데이터 삭제 완료", year);
            }

            // 3. 새로운 데이터 저장
            List<Holiday> savedHolidays = holidayRepository.saveAll(holidays);
            log.info("{}년 공휴일 {}개 저장 완료", year, savedHolidays.size());

            return savedHolidays.size();

        } catch (Exception e) {
            log.error("{}년 공휴일 업데이트 실패: {}", year, e.getMessage(), e);
            throw new RuntimeException("공휴일 정보 업데이트에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 여러 연도의 공휴일 정보 업데이트
     *
     * @param years 연도 배열
     * @return 총 저장된 공휴일 개수
     */
    @Transactional
    public int updateHolidays(int... years) {
        int totalCount = 0;
        for (int year : years) {
            totalCount += updateHolidays(year);
        }
        return totalCount;
    }

    /**
     * 현재 연도와 다음 연도의 공휴일 업데이트
     *
     * @return 총 저장된 공휴일 개수
     */
    @Transactional
    public int updateCurrentAndNextYearHolidays() {
        int currentYear = LocalDate.now().getYear();
        return updateHolidays(currentYear, currentYear + 1);
    }

    /**
     * 공휴일 정보 존재 여부 확인
     *
     * @param year 연도
     * @return 존재 여부
     */
    public boolean hasHolidays(int year) {
        return holidayRepository.existsByYear(year);
    }

    /**
     * 공휴일 개수 조회
     *
     * @param year 연도
     * @return 공휴일 개수
     */
    public long countHolidays(int year) {
        return holidayRepository.countByYear(year);
    }
}
