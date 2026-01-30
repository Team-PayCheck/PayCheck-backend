package com.example.paycheck.domain.allowance.service;

import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyAllowanceService {

    private final WeeklyAllowanceRepository weeklyAllowanceRepository;
    private final WorkerContractRepository workerContractRepository;

    public List<WeeklyAllowance> getWeeklyAllowancesByContract(Long contractId) {
        return weeklyAllowanceRepository.findByContractId(contractId);
    }

    /**
     * WorkRecord 생성/수정 시 호출
     * 해당 주의 WeeklyAllowance를 조회하거나 생성하고, 수당을 재계산
     */
    @Transactional
    public WeeklyAllowance getOrCreateWeeklyAllowanceForDate(Long contractId, LocalDate workDate) {
        // 해당 주에 이미 WeeklyAllowance가 있는지 조회
        Optional<WeeklyAllowance> existingAllowance = weeklyAllowanceRepository.findByContractAndWeek(contractId, workDate);

        if (existingAllowance.isPresent()) {
            // 기존 WeeklyAllowance가 있으면 반환
            return existingAllowance.get();
        }

        // 없으면 새로 생성
        WorkerContract contract = workerContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 해당 날짜가 속한 주의 시작일(월요일)과 종료일(일요일) 계산
        LocalDate weekStart = workDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = workDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        WeeklyAllowance newAllowance = WeeklyAllowance.builder()
                .contract(contract)
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .build();

        return weeklyAllowanceRepository.save(newAllowance);
    }

    /**
     * 특정 WeeklyAllowance의 수당을 재계산
     * WorkRecord 추가/수정/삭제 후 호출
     */
    @Transactional
    public WeeklyAllowance recalculateAllowances(Long weeklyAllowanceId) {
        WeeklyAllowance allowance = weeklyAllowanceRepository.findById(weeklyAllowanceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WEEKLY_ALLOWANCE_NOT_FOUND, "주간 수당 정보를 찾을 수 없습니다."));

        // 사업장 규모 확인
        boolean isSmallWorkplace = allowance.getContract().getWorkplace().getIsLessThanFiveEmployees();

        // 수당 재계산 (사업장 규모 고려)
        allowance.calculateTotalWorkHours();
        allowance.calculateWeeklyPaidLeave();
        allowance.calculateOvertime(isSmallWorkplace);

        return weeklyAllowanceRepository.save(allowance);
    }

    /**
     * WeeklyAllowance 삭제
     * WorkRecord가 모두 삭제되었을 때 호출
     */
    @Transactional
    public void deleteWeeklyAllowance(Long weeklyAllowanceId) {
        weeklyAllowanceRepository.deleteById(weeklyAllowanceId);
    }

    /**
     * 여러 날짜에 대한 WeeklyAllowance를 일괄 조회하거나 생성 (배치 처리용)
     * @return Map<주 시작일(월요일), WeeklyAllowance>
     */
    @Transactional
    public Map<LocalDate, WeeklyAllowance> getOrCreateWeeklyAllowancesForDates(Long contractId, List<LocalDate> workDates) {
        if (workDates.isEmpty()) {
            return new HashMap<>();
        }

        // 날짜 범위 계산
        LocalDate minDate = workDates.stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate maxDate = workDates.stream().max(LocalDate::compareTo).orElseThrow();

        // 기존 WeeklyAllowance 일괄 조회
        List<WeeklyAllowance> existingAllowances = weeklyAllowanceRepository
                .findByContractAndDateRange(contractId, minDate, maxDate);

        // 주 시작일 기준 Map 생성
        Map<LocalDate, WeeklyAllowance> allowanceMap = existingAllowances.stream()
                .collect(Collectors.toMap(
                        WeeklyAllowance::getWeekStartDate,
                        Function.identity(),
                        (existing, replacement) -> existing));

        // 없는 주에 대해 새로 생성
        WorkerContract contract = workerContractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        List<WeeklyAllowance> newAllowances = new ArrayList<>();
        for (LocalDate workDate : workDates) {
            LocalDate weekStart = workDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (!allowanceMap.containsKey(weekStart)) {
                LocalDate weekEnd = workDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                WeeklyAllowance newAllowance = WeeklyAllowance.builder()
                        .contract(contract)
                        .weekStartDate(weekStart)
                        .weekEndDate(weekEnd)
                        .build();
                newAllowances.add(newAllowance);
                allowanceMap.put(weekStart, newAllowance);
            }
        }

        // 새 WeeklyAllowance 일괄 저장
        if (!newAllowances.isEmpty()) {
            weeklyAllowanceRepository.saveAll(newAllowances);
        }

        return allowanceMap;
    }

    /**
     * 여러 WeeklyAllowance의 수당을 일괄 재계산 (배치 처리용)
     */
    @Transactional
    public void recalculateAllowancesBatch(Set<Long> weeklyAllowanceIds) {
        if (weeklyAllowanceIds.isEmpty()) {
            return;
        }

        List<WeeklyAllowance> allowances = weeklyAllowanceRepository.findAllById(weeklyAllowanceIds);

        for (WeeklyAllowance allowance : allowances) {
            boolean isSmallWorkplace = allowance.getContract().getWorkplace().getIsLessThanFiveEmployees();
            allowance.calculateTotalWorkHours();
            allowance.calculateWeeklyPaidLeave();
            allowance.calculateOvertime(isSmallWorkplace);
        }

        weeklyAllowanceRepository.saveAll(allowances);
    }
}
