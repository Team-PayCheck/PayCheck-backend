package com.example.paycheck.domain.holiday.service;

import com.example.paycheck.domain.holiday.entity.Holiday;
import com.example.paycheck.domain.holiday.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HolidayService 테스트")
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private HolidayApiClient holidayApiClient;

    @InjectMocks
    private HolidayService holidayService;

    private Holiday testHoliday;

    @BeforeEach
    void setUp() {
        testHoliday = Holiday.builder()
                .id(1L)
                .holidayDate(LocalDate.of(2024, 1, 1))
                .holidayName("신정")
                .year(2024)
                .month(1)
                .isPublicHoliday(true)
                .build();
    }

    @Test
    @DisplayName("공휴일 확인 - 토요일은 공휴일")
    void isHoliday_Saturday() {
        // given
        LocalDate saturday = LocalDate.of(2024, 1, 6); // 토요일

        // when
        boolean result = holidayService.isHoliday(saturday);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository, never()).existsByHolidayDate(any());
    }

    @Test
    @DisplayName("공휴일 확인 - 일요일은 공휴일")
    void isHoliday_Sunday() {
        // given
        LocalDate sunday = LocalDate.of(2024, 1, 7); // 일요일

        // when
        boolean result = holidayService.isHoliday(sunday);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository, never()).existsByHolidayDate(any());
    }

    @Test
    @DisplayName("공휴일 확인 - DB에 등록된 공휴일")
    void isHoliday_RegisteredHoliday() {
        // given
        LocalDate weekday = LocalDate.of(2024, 1, 1); // 월요일
        when(holidayRepository.existsByHolidayDate(weekday)).thenReturn(true);

        // when
        boolean result = holidayService.isHoliday(weekday);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository).existsByHolidayDate(weekday);
    }

    @Test
    @DisplayName("공휴일 확인 - 평일이고 공휴일 아님")
    void isHoliday_NotHoliday() {
        // given
        LocalDate weekday = LocalDate.of(2024, 1, 2); // 화요일
        when(holidayRepository.existsByHolidayDate(weekday)).thenReturn(false);

        // when
        boolean result = holidayService.isHoliday(weekday);

        // then
        assertThat(result).isFalse();
        verify(holidayRepository).existsByHolidayDate(weekday);
    }

    @Test
    @DisplayName("공공기관 휴일 확인 - 토요일은 공공기관 휴일")
    void isPublicHoliday_Saturday() {
        // given
        LocalDate saturday = LocalDate.of(2024, 1, 6); // 토요일

        // when
        boolean result = holidayService.isPublicHoliday(saturday);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository, never()).existsByHolidayDateAndIsPublicHolidayTrue(any());
    }

    @Test
    @DisplayName("공공기관 휴일 확인 - 일요일은 공공기관 휴일")
    void isPublicHoliday_Sunday() {
        // given
        LocalDate sunday = LocalDate.of(2024, 1, 7); // 일요일

        // when
        boolean result = holidayService.isPublicHoliday(sunday);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository, never()).existsByHolidayDateAndIsPublicHolidayTrue(any());
    }

    @Test
    @DisplayName("공공기관 휴일 확인 - DB에 등록된 공공기관 휴일")
    void isPublicHoliday_RegisteredPublicHoliday() {
        // given
        LocalDate weekday = LocalDate.of(2024, 1, 1); // 월요일 (신정)
        when(holidayRepository.existsByHolidayDateAndIsPublicHolidayTrue(weekday)).thenReturn(true);

        // when
        boolean result = holidayService.isPublicHoliday(weekday);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository).existsByHolidayDateAndIsPublicHolidayTrue(weekday);
    }

    @Test
    @DisplayName("공공기관 휴일 확인 - 기념일(isPublicHoliday=false)은 공공기관 휴일 아님")
    void isPublicHoliday_MemorialDay_NotPublicHoliday() {
        // given
        LocalDate weekday = LocalDate.of(2024, 4, 5); // 식목일
        when(holidayRepository.existsByHolidayDateAndIsPublicHolidayTrue(weekday)).thenReturn(false);

        // when
        boolean result = holidayService.isPublicHoliday(weekday);

        // then
        assertThat(result).isFalse();
        verify(holidayRepository).existsByHolidayDateAndIsPublicHolidayTrue(weekday);
    }

    @Test
    @DisplayName("연도별 공휴일 날짜 조회")
    void getHolidayDates_Success() {
        // given
        int year = 2024;
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 5, 5)
        );
        when(holidayRepository.findAllHolidayDatesByYear(year)).thenReturn(dates);

        // when
        Set<LocalDate> result = holidayService.getHolidayDates(year);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).contains(LocalDate.of(2024, 1, 1));
        verify(holidayRepository).findAllHolidayDatesByYear(year);
    }

    @Test
    @DisplayName("연도별 공휴일 목록 조회")
    void getHolidays_ByYear() {
        // given
        int year = 2024;
        when(holidayRepository.findByYearOrderByHolidayDateAsc(year))
                .thenReturn(Arrays.asList(testHoliday));

        // when
        List<Holiday> result = holidayService.getHolidays(year);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHolidayName()).isEqualTo("신정");
        verify(holidayRepository).findByYearOrderByHolidayDateAsc(year);
    }

    @Test
    @DisplayName("연월별 공휴일 목록 조회")
    void getHolidays_ByYearAndMonth() {
        // given
        int year = 2024;
        int month = 1;
        when(holidayRepository.findByYearAndMonthOrderByHolidayDateAsc(year, month))
                .thenReturn(Arrays.asList(testHoliday));

        // when
        List<Holiday> result = holidayService.getHolidays(year, month);

        // then
        assertThat(result).hasSize(1);
        verify(holidayRepository).findByYearAndMonthOrderByHolidayDateAsc(year, month);
    }

    @Test
    @DisplayName("기간별 공휴일 목록 조회")
    void getHolidaysBetween_Success() {
        // given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        when(holidayRepository.findByDateRange(startDate, endDate))
                .thenReturn(Arrays.asList(testHoliday));

        // when
        List<Holiday> result = holidayService.getHolidaysBetween(startDate, endDate);

        // then
        assertThat(result).hasSize(1);
        verify(holidayRepository).findByDateRange(startDate, endDate);
    }

    @Test
    @DisplayName("공휴일 업데이트 성공")
    void updateHolidays_Success() {
        // given
        int year = 2024;
        List<Holiday> holidays = Arrays.asList(testHoliday);
        when(holidayApiClient.isApiKeyConfigured()).thenReturn(true);
        when(holidayApiClient.fetchHolidays(year)).thenReturn(holidays);
        when(holidayRepository.existsByYear(year)).thenReturn(false);
        when(holidayRepository.saveAll(holidays)).thenReturn(holidays);

        // when
        int result = holidayService.updateHolidays(year);

        // then
        assertThat(result).isEqualTo(1);
        verify(holidayApiClient).fetchHolidays(year);
        verify(holidayRepository).saveAll(holidays);
    }

    @Test
    @DisplayName("공휴일 업데이트 - API 키 미설정")
    void updateHolidays_NoApiKey() {
        // given
        when(holidayApiClient.isApiKeyConfigured()).thenReturn(false);

        // when
        int result = holidayService.updateHolidays(2024);

        // then
        assertThat(result).isEqualTo(0);
        verify(holidayApiClient, never()).fetchHolidays(anyInt());
    }

    @Test
    @DisplayName("공휴일 업데이트 - 기존 데이터 삭제 후 저장")
    void updateHolidays_ReplaceExisting() {
        // given
        int year = 2024;
        List<Holiday> holidays = Arrays.asList(testHoliday);
        when(holidayApiClient.isApiKeyConfigured()).thenReturn(true);
        when(holidayApiClient.fetchHolidays(year)).thenReturn(holidays);
        when(holidayRepository.existsByYear(year)).thenReturn(true);
        when(holidayRepository.saveAll(holidays)).thenReturn(holidays);
        doNothing().when(holidayRepository).deleteByYear(year);

        // when
        int result = holidayService.updateHolidays(year);

        // then
        assertThat(result).isEqualTo(1);
        verify(holidayRepository).deleteByYear(year);
        verify(holidayRepository).saveAll(holidays);
    }

    @Test
    @DisplayName("공휴일 업데이트 - 빈 데이터")
    void updateHolidays_EmptyData() {
        // given
        int year = 2024;
        when(holidayApiClient.isApiKeyConfigured()).thenReturn(true);
        when(holidayApiClient.fetchHolidays(year)).thenReturn(Arrays.asList());

        // when
        int result = holidayService.updateHolidays(year);

        // then
        assertThat(result).isEqualTo(0);
        verify(holidayRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("여러 연도 공휴일 업데이트")
    void updateHolidays_MultipleYears() {
        // given
        when(holidayApiClient.isApiKeyConfigured()).thenReturn(true);
        when(holidayApiClient.fetchHolidays(anyInt())).thenReturn(Arrays.asList(testHoliday));
        when(holidayRepository.existsByYear(anyInt())).thenReturn(false);
        when(holidayRepository.saveAll(any())).thenReturn(Arrays.asList(testHoliday));

        // when
        int result = holidayService.updateHolidays(2024, 2025);

        // then
        assertThat(result).isEqualTo(2);
        verify(holidayApiClient, times(2)).fetchHolidays(anyInt());
    }

    @Test
    @DisplayName("현재 및 다음 연도 공휴일 업데이트")
    void updateCurrentAndNextYearHolidays_Success() {
        // given
        when(holidayApiClient.isApiKeyConfigured()).thenReturn(true);
        when(holidayApiClient.fetchHolidays(anyInt())).thenReturn(Arrays.asList(testHoliday));
        when(holidayRepository.existsByYear(anyInt())).thenReturn(false);
        when(holidayRepository.saveAll(any())).thenReturn(Arrays.asList(testHoliday));

        // when
        int result = holidayService.updateCurrentAndNextYearHolidays();

        // then
        assertThat(result).isEqualTo(2);
        verify(holidayApiClient, times(2)).fetchHolidays(anyInt());
    }

    @Test
    @DisplayName("공휴일 존재 여부 확인 - 존재함")
    void hasHolidays_Exists() {
        // given
        when(holidayRepository.existsByYear(2024)).thenReturn(true);

        // when
        boolean result = holidayService.hasHolidays(2024);

        // then
        assertThat(result).isTrue();
        verify(holidayRepository).existsByYear(2024);
    }

    @Test
    @DisplayName("공휴일 존재 여부 확인 - 존재하지 않음")
    void hasHolidays_NotExists() {
        // given
        when(holidayRepository.existsByYear(2024)).thenReturn(false);

        // when
        boolean result = holidayService.hasHolidays(2024);

        // then
        assertThat(result).isFalse();
        verify(holidayRepository).existsByYear(2024);
    }

    @Test
    @DisplayName("공휴일 개수 조회")
    void countHolidays_Success() {
        // given
        when(holidayRepository.countByYear(2024)).thenReturn(15L);

        // when
        long result = holidayService.countHolidays(2024);

        // then
        assertThat(result).isEqualTo(15L);
        verify(holidayRepository).countByYear(2024);
    }
}
