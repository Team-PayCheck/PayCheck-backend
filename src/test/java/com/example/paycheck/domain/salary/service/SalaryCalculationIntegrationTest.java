package com.example.paycheck.domain.salary.service;

import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.allowance.service.WeeklyAllowanceService;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.employer.entity.Employer;
import com.example.paycheck.domain.employer.repository.EmployerRepository;
import com.example.paycheck.domain.holiday.service.HolidayService;
import com.example.paycheck.domain.salary.dto.SalaryDto;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.salary.util.DeductionCalculator;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.user.enums.UserType;
import com.example.paycheck.domain.user.repository.UserRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.worker.repository.WorkerRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workplace.repository.WorkplaceRepository;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("급여 계산 통합 테스트")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SalaryCalculationIntegrationTest {

    @Autowired private SalaryService salaryService;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployerRepository employerRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private WorkplaceRepository workplaceRepository;
    @Autowired private WorkerContractRepository workerContractRepository;
    @Autowired private WorkRecordRepository workRecordRepository;
    @Autowired private WeeklyAllowanceRepository weeklyAllowanceRepository;
    @Autowired private WeeklyAllowanceService weeklyAllowanceService;
    @Autowired private SalaryRepository salaryRepository;

    // Holiday 테이블이 H2에서 DDL 오류(예약어 month/year)로 생성 불가하므로 mock 처리
    @MockitoBean
    private HolidayService holidayService;

    // createdAt 기준으로 WeeklyAllowance를 조회하므로 현재 년/월 사용
    private final int currentYear = LocalDate.now().getYear();
    private final int currentMonth = LocalDate.now().getMonthValue();

    // 현재 월의 첫 번째 월요일 기준으로 테스트 날짜 설정
    private final LocalDate firstMonday = LocalDate.of(currentYear, currentMonth, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

    @BeforeEach
    void setUpHolidayMock() {
        // 기본적으로 주말만 휴일, 평일은 비휴일
        when(holidayService.isPublicHoliday(any(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return date.getDayOfWeek().getValue() >= 6; // 토/일만 휴일
        });
        when(holidayService.getHolidayDates(anyInt())).thenReturn(Set.of());
    }

    @AfterEach
    void cleanup() {
        salaryRepository.deleteAll();
        workRecordRepository.deleteAll();
        weeklyAllowanceRepository.deleteAll();
        workerContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        employerRepository.deleteAll();
        workerRepository.deleteAll();
        userRepository.deleteAll();
    }

    private WorkerContract setupBaseData(BigDecimal hourlyWage, int paymentDay,
                                          boolean isSmallWorkplace,
                                          DeductionCalculator.PayrollDeductionType deductionType) {
        User employerUser = userRepository.save(User.builder()
                .kakaoId("employer_" + System.nanoTime())
                .name("테스트고용주")
                .userType(UserType.EMPLOYER)
                .build());

        Employer employer = employerRepository.save(Employer.builder()
                .user(employerUser)
                .build());

        Workplace workplace = workplaceRepository.save(Workplace.builder()
                .employer(employer)
                .businessNumber("123-45-" + (System.nanoTime() % 100000))
                .name("테스트사업장")
                .isLessThanFiveEmployees(isSmallWorkplace)
                .build());

        User workerUser = userRepository.save(User.builder()
                .kakaoId("worker_" + System.nanoTime())
                .name("테스트근로자")
                .userType(UserType.WORKER)
                .build());

        Worker worker = workerRepository.save(Worker.builder()
                .user(workerUser)
                .workerCode(String.format("%06d", System.nanoTime() % 1000000))
                .build());

        return workerContractRepository.save(WorkerContract.builder()
                .workplace(workplace)
                .worker(worker)
                .hourlyWage(hourlyWage)
                .workSchedules("[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"18:00\"}]")
                .contractStartDate(LocalDate.of(2024, 1, 1))
                .paymentDay(paymentDay)
                .payrollDeductionType(deductionType)
                .build());
    }

    /**
     * WorkRecord 생성 + 엔티티 계산 메서드 직접 호출 + 저장
     */
    private WorkRecord createAndCalculateWorkRecord(WorkerContract contract, LocalDate workDate,
                                                     LocalTime startTime, LocalTime endTime,
                                                     boolean isHoliday, boolean isSmallWorkplace,
                                                     WeeklyAllowance weeklyAllowance) {
        WorkRecord workRecord = WorkRecord.builder()
                .contract(contract)
                .workDate(workDate)
                .startTime(startTime)
                .endTime(endTime)
                .breakMinutes(0)
                .status(WorkRecordStatus.COMPLETED)
                .weeklyAllowance(weeklyAllowance)
                .build();

        workRecord.calculateHoursWithHolidayInfo(isHoliday, isSmallWorkplace);
        workRecord.calculateSalaryWithAllowanceRules(isSmallWorkplace);

        return workRecordRepository.save(workRecord);
    }

    private WeeklyAllowance createWeeklyAllowance(WorkerContract contract, LocalDate weekStart, LocalDate weekEnd) {
        return weeklyAllowanceRepository.save(WeeklyAllowance.builder()
                .contract(contract)
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .workRecords(new ArrayList<>())
                .build());
    }

    @Test
    @DisplayName("기본 급여 계산 E2E - 평일 8시간 × 3일, PART_TIME_NONE")
    void basicSalaryCalculation_E2E() {
        // given
        WorkerContract contract = setupBaseData(
                new BigDecimal("10000"), 25, false,
                DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        LocalDate weekStart = firstMonday;
        LocalDate weekEnd = weekStart.plusDays(6);
        WeeklyAllowance allowance = createWeeklyAllowance(contract, weekStart, weekEnd);

        // 평일 8시간 × 3일
        createAndCalculateWorkRecord(contract, weekStart,
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);
        createAndCalculateWorkRecord(contract, weekStart.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);
        createAndCalculateWorkRecord(contract, weekStart.plusDays(2),
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(
                contract.getId(), currentYear, currentMonth);

        // then
        assertThat(response.getBasePay()).isEqualByComparingTo("240000"); // 8h × 10000 × 3일
        assertThat(response.getNightPay()).isEqualByComparingTo("0");
        assertThat(response.getHolidayPay()).isEqualByComparingTo("0");
        assertThat(response.getTotalDeduction()).isEqualByComparingTo("0");
        assertThat(response.getNetPay()).isEqualByComparingTo(response.getTotalGrossPay());
    }

    @Test
    @DisplayName("야간+휴일 복합 E2E - 야간 근무 + 토요일 근무, 5인 이상 사업장")
    void nightAndHolidayWork_E2E() {
        // given
        WorkerContract contract = setupBaseData(
                new BigDecimal("10000"), 25, false,
                DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        LocalDate weekStart = firstMonday;
        LocalDate weekEnd = weekStart.plusDays(6);
        WeeklyAllowance allowance = createWeeklyAllowance(contract, weekStart, weekEnd);

        // 평일 야간 근무: 22:00 ~ 06:00 (8시간 전부 야간)
        createAndCalculateWorkRecord(contract, weekStart,
                LocalTime.of(22, 0), LocalTime.of(6, 0), false, false, allowance);

        // 토요일 주간 근무: 09:00 ~ 17:00 (8시간, 휴일)
        LocalDate saturday = weekStart.plusDays(5); // 월요일 + 5 = 토요일
        createAndCalculateWorkRecord(contract, saturday,
                LocalTime.of(9, 0), LocalTime.of(17, 0), true, false, allowance);

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(
                contract.getId(), currentYear, currentMonth);

        // then
        assertThat(response.getNightPay()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.getHolidayPay()).isGreaterThan(BigDecimal.ZERO);

        // totalGrossPay = basePay + nightPay + holidayPay + overtimePay
        BigDecimal expectedTotal = response.getBasePay()
                .add(response.getNightPay())
                .add(response.getHolidayPay())
                .add(response.getOvertimePay());
        assertThat(response.getTotalGrossPay()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("FREELANCER 공제 E2E - 3.3% 공제 정확성 검증")
    void freelancerDeduction_E2E() {
        // given
        WorkerContract contract = setupBaseData(
                new BigDecimal("12500"), 25, false,
                DeductionCalculator.PayrollDeductionType.FREELANCER);

        // 3주에 걸쳐 근무 기록 생성
        for (int week = 0; week < 3; week++) {
            LocalDate weekStart = firstMonday.plusWeeks(week);
            LocalDate weekEnd = weekStart.plusDays(6);

            // paymentDay(25일) 이전 날짜만 사용
            if (weekStart.getDayOfMonth() >= 25) break;

            WeeklyAllowance allowance = createWeeklyAllowance(contract, weekStart, weekEnd);

            for (int day = 0; day < 5; day++) {
                LocalDate workDate = weekStart.plusDays(day);
                // paymentDay 이전 + 같은 달만
                if (workDate.getDayOfMonth() >= 25 || workDate.getMonthValue() != currentMonth) break;
                createAndCalculateWorkRecord(contract, workDate,
                        LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);
            }
        }

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(
                contract.getId(), currentYear, currentMonth);

        // then
        assertThat(response.getTotalGrossPay()).isGreaterThan(BigDecimal.ZERO);
        // FREELANCER: 소득세 3%, 지방소득세 0.3%
        BigDecimal expectedIncomeTax = response.getTotalGrossPay()
                .multiply(new BigDecimal("0.03")).setScale(0, java.math.RoundingMode.DOWN);
        BigDecimal expectedLocalTax = response.getTotalGrossPay()
                .multiply(new BigDecimal("0.003")).setScale(0, java.math.RoundingMode.DOWN);

        assertThat(response.getIncomeTax()).isEqualByComparingTo(expectedIncomeTax);
        assertThat(response.getLocalIncomeTax()).isEqualByComparingTo(expectedLocalTax);
        assertThat(response.getNetPay()).isEqualByComparingTo(
                response.getTotalGrossPay().subtract(response.getTotalDeduction()));
    }

    @Test
    @DisplayName("주휴수당 포함 E2E - 주 24시간 근무 시 주휴수당이 totalGrossPay에 반영")
    void weeklyPaidLeave_E2E() {
        // given
        WorkerContract contract = setupBaseData(
                new BigDecimal("10000"), 25, false,
                DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        LocalDate weekStart = firstMonday;
        LocalDate weekEnd = weekStart.plusDays(6);
        WeeklyAllowance allowance = createWeeklyAllowance(contract, weekStart, weekEnd);

        // 평일 8시간 × 3일 = 24시간 (주휴수당 조건 15시간 이상 충족)
        createAndCalculateWorkRecord(contract, weekStart,
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);
        createAndCalculateWorkRecord(contract, weekStart.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);
        createAndCalculateWorkRecord(contract, weekStart.plusDays(2),
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);

        // WeeklyAllowance 수당 계산 (서비스 사용 - @Transactional 내에서 lazy loading 처리)
        weeklyAllowanceService.recalculateAllowances(allowance.getId());

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(
                contract.getId(), currentYear, currentMonth);

        // then
        assertThat(response.getBasePay()).isEqualByComparingTo("240000");
        // totalGrossPay > basePay (주휴수당 포함)
        assertThat(response.getTotalGrossPay()).isGreaterThan(response.getBasePay());
    }

    @Test
    @DisplayName("재계산 E2E - WorkRecord 추가 후 재계산 시 급여 변경")
    void recalculation_E2E() {
        // given
        WorkerContract contract = setupBaseData(
                new BigDecimal("10000"), 25, false,
                DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        LocalDate weekStart = firstMonday;
        LocalDate weekEnd = weekStart.plusDays(6);
        WeeklyAllowance allowance = createWeeklyAllowance(contract, weekStart, weekEnd);

        createAndCalculateWorkRecord(contract, weekStart,
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);

        // 첫 번째 계산
        SalaryDto.Response firstResponse = salaryService.calculateSalaryByWorkRecords(
                contract.getId(), currentYear, currentMonth);
        BigDecimal firstBasePay = firstResponse.getBasePay();

        // when - 1일 추가 + 재계산
        createAndCalculateWorkRecord(contract, weekStart.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), false, false, allowance);

        SalaryDto.Response secondResponse = salaryService.recalculateSalaryAfterWorkRecordUpdate(
                contract.getId(), currentYear, currentMonth);

        // then
        assertThat(secondResponse.getBasePay()).isGreaterThan(firstBasePay);
        assertThat(secondResponse.getBasePay()).isEqualByComparingTo("160000"); // 8h × 10000 × 2일
    }
}
