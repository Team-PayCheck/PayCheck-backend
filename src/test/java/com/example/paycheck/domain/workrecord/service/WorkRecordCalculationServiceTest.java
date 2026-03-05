package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.holiday.service.HolidayService;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordCalculationService 테스트")
class WorkRecordCalculationServiceTest {

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private WorkRecordCalculationService calculationService;

    private WorkerContract mockContract;
    private Workplace mockWorkplace;

    @BeforeEach
    void setUp() {
        mockContract = mock(WorkerContract.class);
        mockWorkplace = mock(Workplace.class);
        lenient().when(mockContract.getHourlyWage()).thenReturn(BigDecimal.valueOf(10000));
        lenient().when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
    }

    private WorkRecord buildWorkRecord(LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        return WorkRecord.builder()
                .contract(mockContract)
                .workDate(workDate)
                .startTime(startTime)
                .endTime(endTime)
                .breakMinutes(0)
                .status(WorkRecordStatus.COMPLETED)
                .build();
    }

    @Nested
    @DisplayName("calculateWorkRecordDetails - 단건 계산")
    class CalculateWorkRecordDetails {

        @Test
        @DisplayName("평일 일반 근무 - 공휴일이 아닌 경우 baseSalary만 발생")
        void weekday_NormalWork_BaseSalaryOnly() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.isPublicHoliday(LocalDate.of(2024, 1, 15))).thenReturn(false); // 월요일

            WorkRecord workRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 15),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetails(workRecord);

            // then
            verify(holidayService).isPublicHoliday(LocalDate.of(2024, 1, 15));
            assertThat(workRecord.getBaseSalary()).isEqualByComparingTo(new BigDecimal("80000")); // 8h × 10000
            assertThat(workRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(workRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("공휴일(평일 법정공휴일) - 휴일수당 발생")
        void publicHoliday_Weekday_HolidaySalary() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.isPublicHoliday(LocalDate.of(2024, 1, 1))).thenReturn(true); // 신정(월요일)

            WorkRecord workRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 1),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetails(workRecord);

            // then
            assertThat(workRecord.getHolidaySalary()).isGreaterThan(BigDecimal.ZERO);
            assertThat(workRecord.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO); // 휴일은 baseSalary 없음
        }

        @Test
        @DisplayName("주말(토요일) - 휴일수당 발생")
        void saturday_HolidaySalary() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.isPublicHoliday(LocalDate.of(2024, 1, 13))).thenReturn(true); // 토요일

            WorkRecord workRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 13),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetails(workRecord);

            // then
            assertThat(workRecord.getHolidaySalary()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("5인 미만 사업장 + 공휴일 - 할증 없이 기본급만 발생")
        void smallWorkplace_Holiday_NoSurcharge() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(true);
            when(holidayService.isPublicHoliday(LocalDate.of(2024, 1, 14))).thenReturn(true); // 일요일

            WorkRecord workRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 14),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetails(workRecord);

            // then
            assertThat(workRecord.getBaseSalary()).isEqualByComparingTo(new BigDecimal("80000")); // 8h × 10000 (1.0배)
            assertThat(workRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(workRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("사업장 규모 확인을 위해 workplace.getIsLessThanFiveEmployees() 호출됨")
        void verifyWorkplaceSizeCheck() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.isPublicHoliday(any(LocalDate.class))).thenReturn(false);

            WorkRecord workRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 15),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetails(workRecord);

            // then
            verify(mockWorkplace).getIsLessThanFiveEmployees();
        }
    }

    @Nested
    @DisplayName("calculateWorkRecordDetailsBatch - 배치 계산")
    class CalculateWorkRecordDetailsBatch {

        @Test
        @DisplayName("빈 리스트 입력 시 holidayService 미호출")
        void emptyList_NoHolidayServiceCall() {
            // when
            calculationService.calculateWorkRecordDetailsBatch(Collections.emptyList());

            // then
            verify(holidayService, never()).getHolidayDates(anyInt());
        }

        @Test
        @DisplayName("단일 연도 WorkRecord 3개 - getHolidayDates 1회만 호출 (N+1 방지)")
        void singleYear_ThreeRecords_OneHolidayCall() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.getHolidayDates(2024)).thenReturn(Set.of());

            List<WorkRecord> records = List.of(
                    buildWorkRecord(LocalDate.of(2024, 1, 15), LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    buildWorkRecord(LocalDate.of(2024, 3, 11), LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    buildWorkRecord(LocalDate.of(2024, 6, 10), LocalTime.of(9, 0), LocalTime.of(17, 0))
            );

            // when
            calculationService.calculateWorkRecordDetailsBatch(records);

            // then
            verify(holidayService, times(1)).getHolidayDates(2024);
        }

        @Test
        @DisplayName("복수 연도(2023+2024) - 연도별 1회씩 호출")
        void multipleYears_OneCallPerYear() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.getHolidayDates(2023)).thenReturn(Set.of());
            when(holidayService.getHolidayDates(2024)).thenReturn(Set.of());

            List<WorkRecord> records = List.of(
                    buildWorkRecord(LocalDate.of(2023, 12, 11), LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    buildWorkRecord(LocalDate.of(2023, 12, 18), LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    buildWorkRecord(LocalDate.of(2024, 1, 15), LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    buildWorkRecord(LocalDate.of(2024, 2, 12), LocalTime.of(9, 0), LocalTime.of(17, 0))
            );

            // when
            calculationService.calculateWorkRecordDetailsBatch(records);

            // then
            verify(holidayService, times(1)).getHolidayDates(2023);
            verify(holidayService, times(1)).getHolidayDates(2024);
        }

        @Test
        @DisplayName("토요일 WorkRecord - DB 공휴일 Set에 없어도 휴일 판정 (getDayOfWeek >= 6)")
        void saturday_HolidayEvenIfNotInDb() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.getHolidayDates(2024)).thenReturn(Set.of()); // 빈 공휴일 Set

            WorkRecord saturdayRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 13), // 토요일
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetailsBatch(List.of(saturdayRecord));

            // then - 토요일이므로 휴일수당 발생
            assertThat(saturdayRecord.getHolidaySalary()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("평일 공휴일 - holidayDates Set에 포함된 날짜는 휴일 판정")
        void weekdayHoliday_InHolidaySet_TreatedAsHoliday() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            LocalDate newYear = LocalDate.of(2024, 1, 1); // 월요일 신정
            when(holidayService.getHolidayDates(2024)).thenReturn(Set.of(newYear));

            WorkRecord holidayRecord = buildWorkRecord(
                    newYear,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));

            // when
            calculationService.calculateWorkRecordDetailsBatch(List.of(holidayRecord));

            // then - 공휴일이므로 휴일수당 발생
            assertThat(holidayRecord.getHolidaySalary()).isGreaterThan(BigDecimal.ZERO);
            assertThat(holidayRecord.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("validateWorkRecordConsistency - 정합성 검증")
    class ValidateWorkRecordConsistency {

        @Test
        @DisplayName("정상 데이터 - 합계 일치 시 예외 없음")
        void validConsistency_NoException() {
            // given
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);
            when(holidayService.isPublicHoliday(any(LocalDate.class))).thenReturn(false);

            WorkRecord workRecord = buildWorkRecord(
                    LocalDate.of(2024, 1, 15),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0));
            calculationService.calculateWorkRecordDetails(workRecord);

            // when & then - 계산 직후 정합성 검증 통과
            calculationService.validateWorkRecordConsistency(workRecord);
        }

        @Test
        @DisplayName("totalSalary != baseSalary + nightSalary + holidaySalary → IllegalStateException")
        void inconsistentTotalSalary_ThrowsException() {
            // given
            WorkRecord workRecord = mock(WorkRecord.class);
            when(workRecord.getTotalWorkMinutes()).thenReturn(480);
            when(workRecord.getTotalHours()).thenReturn(BigDecimal.valueOf(8));
            when(workRecord.getBaseSalary()).thenReturn(BigDecimal.valueOf(80000));
            when(workRecord.getNightSalary()).thenReturn(BigDecimal.ZERO);
            when(workRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);
            when(workRecord.getTotalSalary()).thenReturn(BigDecimal.valueOf(90000)); // 불일치

            // when & then
            assertThatThrownBy(() -> calculationService.validateWorkRecordConsistency(workRecord))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("급여 정합성 검증 실패");
        }

        @Test
        @DisplayName("totalWorkMinutes가 null이면 IllegalStateException")
        void nullTotalWorkMinutes_ThrowsException() {
            // given
            WorkRecord workRecord = mock(WorkRecord.class);
            when(workRecord.getTotalWorkMinutes()).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> calculationService.validateWorkRecordConsistency(workRecord))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("totalWorkMinutes=null");
        }

        @Test
        @DisplayName("totalWorkMinutes가 음수이면 IllegalStateException")
        void negativeTotalWorkMinutes_ThrowsException() {
            // given
            WorkRecord workRecord = mock(WorkRecord.class);
            when(workRecord.getTotalWorkMinutes()).thenReturn(-1);

            // when & then
            assertThatThrownBy(() -> calculationService.validateWorkRecordConsistency(workRecord))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("totalWorkMinutes=-1");
        }

        @Test
        @DisplayName("baseSalary가 null이면 IllegalStateException")
        void nullBaseSalary_ThrowsException() {
            // given
            WorkRecord workRecord = mock(WorkRecord.class);
            when(workRecord.getTotalWorkMinutes()).thenReturn(480);
            when(workRecord.getTotalHours()).thenReturn(BigDecimal.valueOf(8));
            when(workRecord.getBaseSalary()).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> calculationService.validateWorkRecordConsistency(workRecord))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("baseSalary=null");
        }
    }
}
