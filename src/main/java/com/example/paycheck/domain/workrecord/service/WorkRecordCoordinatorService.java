package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.allowance.service.WeeklyAllowanceService;
import com.example.paycheck.domain.salary.service.SalaryService;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * 근무 기록과 다른 도메인(WeeklyAllowance, Salary) 간의 협력을 조율하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkRecordCoordinatorService {

    private final WeeklyAllowanceService weeklyAllowanceService;
    private final WeeklyAllowanceRepository weeklyAllowanceRepository;
    private final SalaryService salaryService;

    /**
     * 근무 기록 생성 시 WeeklyAllowance 연동 처리
     * SCHEDULED 또는 COMPLETED 상태로 생성되므로 급여 재계산
     * DELETED 상태는 수당 재계산 제외
     */
    public void handleWorkRecordCreation(WorkRecord workRecord) {
        // 양방향 관계 동기화
        workRecord.addToWeeklyAllowance();

        // DELETED 상태는 WeeklyAllowance 재계산 제외
        if (workRecord.getStatus() != WorkRecordStatus.DELETED) {
            // WeeklyAllowance의 수당 재계산 (SCHEDULED, COMPLETED만 주휴수당 계산에 포함)
            weeklyAllowanceService.recalculateAllowances(workRecord.getWeeklyAllowance().getId());
        }

        // 이번 주 근무 기록 변경이 이전 주 주휴수당에 영향을 줄 수 있으므로 이전 주도 재계산
        recalculatePreviousWeekAllowance(workRecord);
    }

    /**
     * 여러 근무 기록 생성 시 WeeklyAllowance 연동 처리
     * SCHEDULED 상태로 생성되므로 급여 재계산 불필요
     */
    public void handleBatchWorkRecordCreation(List<WorkRecord> workRecords) {
        // 양방향 관계 동기화
        workRecords.forEach(WorkRecord::addToWeeklyAllowance);

        // 각 주의 WeeklyAllowance 수당 재계산 (SCHEDULED도 주휴수당 계산에 포함)
        workRecords.stream()
                .map(WorkRecord::getWeeklyAllowance)
                .distinct()
                .forEach(allowance -> weeklyAllowanceService.recalculateAllowances(allowance.getId()));
    }

    /**
     * 근무 기록 수정 시 WeeklyAllowance 재할당 및 재계산 처리
     * COMPLETED 상태일 때만 급여 재계산
     */
    public void handleWorkRecordUpdate(WorkRecord workRecord, WeeklyAllowance oldWeeklyAllowance, WeeklyAllowance newWeeklyAllowance) {
        handleWorkRecordUpdate(workRecord, oldWeeklyAllowance, newWeeklyAllowance, workRecord.getWorkDate());
    }

    /**
     * 근무 기록 수정 시 원래 근무일을 기준으로 WeeklyAllowance/급여 재계산 처리
     */
    public void handleWorkRecordUpdate(
            WorkRecord workRecord,
            WeeklyAllowance oldWeeklyAllowance,
            WeeklyAllowance newWeeklyAllowance,
            LocalDate originalWorkDate) {
        // 기존 WeeklyAllowance 수당 재계산 (다른 WeeklyAllowance였다면)
        if (oldWeeklyAllowance != null && newWeeklyAllowance != null && !oldWeeklyAllowance.getId().equals(newWeeklyAllowance.getId())) {
            weeklyAllowanceService.recalculateAllowances(oldWeeklyAllowance.getId());
        }

        // 새로운 WeeklyAllowance 수당 재계산 (null이 아닐 때만)
        if (newWeeklyAllowance != null) {
            weeklyAllowanceService.recalculateAllowances(newWeeklyAllowance.getId());
        }

        if (!originalWorkDate.equals(workRecord.getWorkDate())) {
            recalculatePreviousWeekAllowances(workRecord.getContract().getId(), originalWorkDate, workRecord.getWorkDate());
        }

        // COMPLETED 상태일 때만 급여 재계산
        if (workRecord.getStatus() == WorkRecordStatus.COMPLETED) {
            recalculateSalaryForWorkRecordUpdate(workRecord, originalWorkDate);
        }
    }

    /**
     * 근무 기록 삭제 시 WeeklyAllowance 정리 및 재계산 처리
     * COMPLETED 상태의 근무 기록이 삭제되면 급여도 재계산
     */
    public void handleWorkRecordDeletion(WeeklyAllowance weeklyAllowance, WorkRecord workRecord, WorkRecordStatus deletedStatus) {
        // WeeklyAllowance가 비어있으면 삭제
        if (weeklyAllowance != null) {
            // 양방향 관계가 이미 해제되었으므로 컬렉션만 확인
            if (weeklyAllowance.getWorkRecords().isEmpty()) {
                // WorkRecord가 없으면 WeeklyAllowance 삭제
                weeklyAllowanceService.deleteWeeklyAllowance(weeklyAllowance.getId());
            } else {
                // WorkRecord가 남아있으면 수당 재계산
                weeklyAllowanceService.recalculateAllowances(weeklyAllowance.getId());
            }
        }

        // COMPLETED 상태의 근무 기록이 삭제된 경우 급여 재계산
        if (deletedStatus == WorkRecordStatus.COMPLETED) {
            recalculateSalaryForWorkRecord(workRecord);
        }

        // 이번 주 근무 기록 삭제가 이전 주 주휴수당에 영향을 줄 수 있으므로 이전 주도 재계산
        recalculatePreviousWeekAllowance(workRecord);
    }

    /**
     * 근무 완료 처리 시 급여 재계산
     * SCHEDULED → COMPLETED 상태 변경 시에만 급여에 반영
     */
    public void handleWorkRecordCompletion(WorkRecord workRecord) {
        // 급여 재계산 (COMPLETED 상태가 된 근무 기록이 급여에 포함됨)
        recalculateSalaryForWorkRecord(workRecord);
    }

    /**
     * 근무 기록 변경 시 해당 월의 급여 재계산
     * workDate와 paymentDay를 기준으로 해당 급여의 year/month를 계산하여 재계산
     */
    private void recalculateSalaryForWorkRecord(WorkRecord workRecord) {
        recalculateSalaryForDate(
                workRecord.getContract().getId(),
                workRecord.getContract().getPaymentDay(),
                workRecord.getWorkDate());
    }

    private void recalculateSalaryForWorkRecordUpdate(WorkRecord workRecord, LocalDate originalWorkDate) {
        Long contractId = workRecord.getContract().getId();
        Integer paymentDay = workRecord.getContract().getPaymentDay();
        LocalDate updatedWorkDate = workRecord.getWorkDate();

        if (!getSalaryPeriodKey(paymentDay, originalWorkDate).equals(getSalaryPeriodKey(paymentDay, updatedWorkDate))) {
            recalculateSalaryForDate(contractId, paymentDay, originalWorkDate);
        }

        recalculateSalaryForDate(contractId, paymentDay, updatedWorkDate);
    }

    /**
     * 특정 날짜 기준으로 해당 월의 급여 재계산
     * 계약 종료 등 WorkRecord 없이 날짜 기준으로 재계산이 필요한 경우 사용
     */
    public void recalculateSalaryForDate(Long contractId, Integer paymentDay, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        if (date.getDayOfMonth() >= paymentDay) {
            LocalDate nextMonth = date.plusMonths(1);
            year = nextMonth.getYear();
            month = nextMonth.getMonthValue();
        }

        try {
            salaryService.recalculateSalaryAfterWorkRecordUpdate(contractId, year, month);
        } catch (NotFoundException e) {
            // 급여가 아직 생성되지 않은 경우 무시 (정상 케이스)
        }
    }

    /**
     * 이전 주의 WeeklyAllowance 재계산
     * 이번 주 근무 기록 변경이 이전 주의 주휴수당(다음 주 근무 여부 기반)에 영향을 줌
     */
    private void recalculatePreviousWeekAllowance(WorkRecord workRecord) {
        LocalDate workDate = workRecord.getWorkDate();
        LocalDate previousWeekStart = workDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);

        weeklyAllowanceRepository.findByContractAndWeek(
                workRecord.getContract().getId(), previousWeekStart)
            .ifPresent(prevAllowance ->
                weeklyAllowanceService.recalculateAllowances(prevAllowance.getId()));
    }

    private void recalculatePreviousWeekAllowances(Long contractId, LocalDate... workDates) {
        Stream.of(workDates)
                .map(this::getPreviousWeekStart)
                .distinct()
                .forEach(previousWeekStart ->
                        weeklyAllowanceRepository.findByContractAndWeek(contractId, previousWeekStart)
                                .ifPresent(previousAllowance ->
                                        weeklyAllowanceService.recalculateAllowances(previousAllowance.getId())));
    }

    private LocalDate getPreviousWeekStart(LocalDate workDate) {
        return workDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
    }

    private String getSalaryPeriodKey(Integer paymentDay, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        if (date.getDayOfMonth() >= paymentDay) {
            LocalDate nextMonth = date.plusMonths(1);
            year = nextMonth.getYear();
            month = nextMonth.getMonthValue();
        }

        return year + "_" + month;
    }

    /**
     * 특정 날짜에 대한 WeeklyAllowance를 가져오거나 생성
     */
    public WeeklyAllowance getOrCreateWeeklyAllowance(Long contractId, LocalDate workDate) {
        return weeklyAllowanceService.getOrCreateWeeklyAllowanceForDate(contractId, workDate);
    }

    /**
     * 여러 날짜에 대한 WeeklyAllowance를 일괄 조회/생성 (배치 처리용)
     */
    public Map<LocalDate, WeeklyAllowance> getOrCreateWeeklyAllowances(Long contractId, List<LocalDate> workDates) {
        return weeklyAllowanceService.getOrCreateWeeklyAllowancesForDates(contractId, workDates);
    }

    /**
     * 일괄 생성된 WorkRecord들의 WeeklyAllowance 수당 일괄 재계산 (배치 처리용)
     */
    public void recalculateWeeklyAllowancesBatch(Set<Long> weeklyAllowanceIds) {
        weeklyAllowanceService.recalculateAllowancesBatch(weeklyAllowanceIds);
    }

    /**
     * COMPLETED 상태 WorkRecord들의 급여 일괄 재계산 (배치 처리용)
     * 동일 계약/년월별로 그룹핑하여 급여 재계산 최소화
     */
    public void handleBatchWorkRecordCompletion(List<WorkRecord> completedRecords) {
        if (completedRecords.isEmpty()) {
            return;
        }

        // 동일 계약/년월별로 그룹핑하여 급여 재계산 최소화
        Map<String, WorkRecord> groupedRecords = completedRecords.stream()
                .collect(Collectors.toMap(
                        wr -> {
                            LocalDate workDate = wr.getWorkDate();
                            Integer paymentDay = wr.getContract().getPaymentDay();
                            int year = workDate.getYear();
                            int month = workDate.getMonthValue();
                            if (workDate.getDayOfMonth() >= paymentDay) {
                                LocalDate nextMonth = workDate.plusMonths(1);
                                year = nextMonth.getYear();
                                month = nextMonth.getMonthValue();
                            }
                            return wr.getContract().getId() + "_" + year + "_" + month;
                        },
                        wr -> wr,
                        (existing, replacement) -> existing));

        // 그룹별로 한 번씩만 급여 재계산
        for (WorkRecord sample : groupedRecords.values()) {
            LocalDate workDate = sample.getWorkDate();
            Integer paymentDay = sample.getContract().getPaymentDay();
            int year = workDate.getYear();
            int month = workDate.getMonthValue();
            if (workDate.getDayOfMonth() >= paymentDay) {
                LocalDate nextMonth = workDate.plusMonths(1);
                year = nextMonth.getYear();
                month = nextMonth.getMonthValue();
            }

            try {
                salaryService.recalculateSalaryAfterWorkRecordUpdate(
                        sample.getContract().getId(), year, month);
            } catch (NotFoundException e) {
                // 급여가 아직 생성되지 않은 경우 무시
            }
        }
    }

}
