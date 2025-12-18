package com.example.wagemanager.domain.salary.service;

import com.example.wagemanager.common.exception.ErrorCode;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
import com.example.wagemanager.domain.salary.dto.SalaryDto;
import com.example.wagemanager.domain.salary.entity.Salary;
import com.example.wagemanager.domain.salary.repository.SalaryRepository;
import com.example.wagemanager.domain.salary.util.DeductionCalculator;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final WorkRecordRepository workRecordRepository;
    private final WorkerContractRepository workerContractRepository;
    private final WeeklyAllowanceRepository weeklyAllowanceRepository;

    /**
     * 급여 상세 조회
     */
    public SalaryDto.Response getSalaryById(Long salaryId) {
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_NOT_FOUND, "급여 정보를 찾을 수 없습니다."));
        return SalaryDto.Response.from(salary);
    }

    /**
     * 사업장별 월별 급여 목록 조회
     */
    public List<SalaryDto.ListResponse> getSalariesByWorkplace(Long workplaceId) {
        return salaryRepository.findByWorkplaceId(workplaceId)
                .stream()
                .map(SalaryDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사업장별 연월 급여 목록 조회
     */
    public List<SalaryDto.ListResponse> getSalariesByWorkplaceAndYearMonth(Long workplaceId, Integer year, Integer month) {
        return salaryRepository.findByWorkplaceId(workplaceId)
                .stream()
                .filter(s -> s.getYear().equals(year) && s.getMonth().equals(month))
                .map(SalaryDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 근로자별 급여 목록 조회
     */
    public List<SalaryDto.ListResponse> getSalariesByWorker(Long workerId) {
        return salaryRepository.findByWorkerId(workerId)
                .stream()
                .map(SalaryDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 계약별 급여 목록 조회
     */
    public List<SalaryDto.ListResponse> getSalariesByContract(Long contractId) {
        return salaryRepository.findByContractId(contractId)
                .stream()
                .map(SalaryDto.ListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 급여 자동 계산 (실시간 근무 기록 기반)
     * - 월급날 기준으로 급여 계산 (전월 paymentDay ~ 당월 paymentDay-1)
     * - 해당 기간의 모든 근무 기록을 조회
     * - 각 근무 기록의 급여를 합산
     * - 세금/보험료를 계산하여 순급여 도출
     */
    @Transactional
    public SalaryDto.Response calculateSalaryByWorkRecords(Long contractId, Integer year, Integer month) {
        WorkerContract contract = workerContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 월급날 기준으로 급여 계산 기간 설정
        // 예: 월급날이 21일이면, 전월 21일 ~ 당월 20일까지
        Integer paymentDay = contract.getPaymentDay();
        LocalDate startDate = LocalDate.of(year, month, 1).minusMonths(1).withDayOfMonth(paymentDay);
        LocalDate endDate = LocalDate.of(year, month, 1).withDayOfMonth(paymentDay).minusDays(1);

        List<WorkRecord> workRecords = workRecordRepository.findByContractAndDateRange(
                contractId, startDate, endDate);

        // 기간 내 WorkRecord가 없으면 Salary 생성하지 않음
        if (workRecords.isEmpty()) {
            throw new NotFoundException(ErrorCode.WORK_RECORD_NOT_FOUND, "해당 기간 내 근무 기록이 없습니다.");
        }

        // WorkRecord의 이미 계산된 급여 칼럼값 합산
        BigDecimal totalWorkHours = BigDecimal.ZERO;
        BigDecimal totalBasePay = BigDecimal.ZERO;
        BigDecimal totalNightPay = BigDecimal.ZERO;
        BigDecimal totalHolidayPay = BigDecimal.ZERO;

        for (WorkRecord record : workRecords) {
            totalWorkHours = totalWorkHours.add(record.getTotalHours());
            totalBasePay = totalBasePay.add(record.getBaseSalary());
            totalNightPay = totalNightPay.add(record.getNightSalary());
            totalHolidayPay = totalHolidayPay.add(record.getHolidaySalary());
        }

        // ========================================
        // 주휴수당 및 연장수당 계산 (마지막 주차 이월 정책 적용)
        // ========================================

        // 당월 WeeklyAllowance 조회
        List<WeeklyAllowance> weeklyAllowances = weeklyAllowanceRepository.findByContractIdAndYearMonth(contractId, year, month);
        BigDecimal totalWeeklyPaidLeaveAmount = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;

        // 월급날 계산 (당월 paymentDay)
        LocalDate paymentDayDate = LocalDate.of(year, month, paymentDay);

        // 당월 WeeklyAllowance 처리
        for (WeeklyAllowance allowance : weeklyAllowances) {
            // 마지막 주차 판단: 월급날이 해당 주(weekStartDate ~ weekEndDate)에 포함되는지 확인
            boolean isLastWeek = !paymentDayDate.isBefore(allowance.getWeekStartDate())
                              && !paymentDayDate.isAfter(allowance.getWeekEndDate());

            if (!isLastWeek) {
                // 마지막 주차가 아니면 현재 월 급여에 포함
                totalWeeklyPaidLeaveAmount = totalWeeklyPaidLeaveAmount.add(allowance.getWeeklyPaidLeaveAmount());
                totalOvertimePay = totalOvertimePay.add(allowance.getOvertimeAmount());
            }
            // 마지막 주차면 제외 (다음 달 급여로 이월)
        }

        // 전월에서 이월된 수당 포함
        LocalDate previousMonth = paymentDayDate.minusMonths(1);
        List<WeeklyAllowance> previousMonthAllowances = weeklyAllowanceRepository.findByContractIdAndYearMonth(
                contractId, previousMonth.getYear(), previousMonth.getMonthValue());

        LocalDate previousPaymentDayDate = LocalDate.of(previousMonth.getYear(), previousMonth.getMonthValue(), paymentDay);

        for (WeeklyAllowance allowance : previousMonthAllowances) {
            // 전월의 마지막 주차(전월 월급날이 포함된 주)를 찾아서 현재 월 급여에 포함
            boolean isPreviousLastWeek = !previousPaymentDayDate.isBefore(allowance.getWeekStartDate())
                                      && !previousPaymentDayDate.isAfter(allowance.getWeekEndDate());

            if (isPreviousLastWeek) {
                // 전월 마지막 주차의 수당을 현재 월 급여에 추가 (이월분)
                totalWeeklyPaidLeaveAmount = totalWeeklyPaidLeaveAmount.add(allowance.getWeeklyPaidLeaveAmount());
                totalOvertimePay = totalOvertimePay.add(allowance.getOvertimeAmount());
            }
        }

        BigDecimal totalGrossPay = totalBasePay.add(totalNightPay).add(totalHolidayPay)
                .add(totalWeeklyPaidLeaveAmount).add(totalOvertimePay);

        // 세금 및 보험료 계산 (payrollDeductionType에 따라)
        // 근무 기록은 있지만 급여가 0원이면 4대보험 면제
        DeductionCalculator.PayrollDeductionType deductionType = contract.getPayrollDeductionType();

        if (deductionType == DeductionCalculator.PayrollDeductionType.PART_TIME_TAX_AND_INSURANCE
            && totalGrossPay.compareTo(BigDecimal.ZERO) == 0) {
            // 근무하지 않은 경우(급여 0원) 4대보험 면제
            deductionType = DeductionCalculator.PayrollDeductionType.PART_TIME_NONE;
        }

        DeductionCalculator.TaxResult taxResult = DeductionCalculator.calculate(totalGrossPay, deductionType);

        BigDecimal fourMajorInsurance = taxResult.totalInsurance;
        BigDecimal incomeTax = taxResult.incomeTax;
        BigDecimal localIncomeTax = taxResult.localIncomeTax;
        BigDecimal totalDeduction = taxResult.totalDeduction;

        BigDecimal netPay = totalGrossPay.subtract(totalDeduction);

        // 음수 급여 방지 (공제액이 급여를 초과하는 경우)
        if (netPay.compareTo(BigDecimal.ZERO) < 0) {
            netPay = BigDecimal.ZERO;
        }

        // 기존 급여 정보 확인 또는 새로 생성
        List<Salary> existingSalaries = salaryRepository.findByContractIdAndYearAndMonth(contractId, year, month);
        Salary salary;

        if (!existingSalaries.isEmpty()) {
            // 기존 급여 정보 업데이트
            salary = existingSalaries.get(0);
            salary = Salary.builder()
                    .id(salary.getId())
                    .contract(contract)
                    .year(year)
                    .month(month)
                    .totalWorkHours(totalWorkHours)
                    .basePay(totalBasePay)
                    .overtimePay(totalOvertimePay)
                    .nightPay(totalNightPay)
                    .holidayPay(totalHolidayPay)
                    .totalGrossPay(totalGrossPay)
                    .fourMajorInsurance(fourMajorInsurance)
                    .incomeTax(incomeTax)
                    .localIncomeTax(localIncomeTax)
                    .totalDeduction(totalDeduction)
                    .netPay(netPay)
                    .paymentDueDate(salary.getPaymentDueDate())
                    .build();
        } else {
            // 새로운 급여 생성
            salary = Salary.builder()
                    .contract(contract)
                    .year(year)
                    .month(month)
                    .totalWorkHours(totalWorkHours)
                    .basePay(totalBasePay)
                    .overtimePay(totalOvertimePay)
                    .nightPay(totalNightPay)
                    .holidayPay(totalHolidayPay)
                    .totalGrossPay(totalGrossPay)
                    .fourMajorInsurance(fourMajorInsurance)
                    .incomeTax(incomeTax)
                    .localIncomeTax(localIncomeTax)
                    .totalDeduction(totalDeduction)
                    .netPay(netPay)
                    .paymentDueDate(LocalDate.of(year, month, contract.getPaymentDay()))
                    .build();
        }

        salaryRepository.save(salary);
        return SalaryDto.Response.from(salary);
    }

    /**
     * 근무 기록 수정 시 해당 월의 급여 자동 재계산
     */
    @Transactional
    public SalaryDto.Response recalculateSalaryAfterWorkRecordUpdate(Long contractId, Integer year, Integer month) {
        return calculateSalaryByWorkRecords(contractId, year, month);
    }
}
