package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.allowance.service.WeeklyAllowanceService;
import com.example.wagemanager.domain.salary.service.SalaryService;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 근무 기록과 다른 도메인(WeeklyAllowance, Salary) 간의 협력을 조율하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkRecordCoordinatorService {

    private final WeeklyAllowanceService weeklyAllowanceService;
    private final SalaryService salaryService;

    /**
     * 근무 기록 생성 시 WeeklyAllowance 연동 처리
     * SCHEDULED 상태로 생성되므로 급여 재계산 불필요
     * PENDING_APPROVAL 상태는 수당 재계산 제외 (승인 후 재계산)
     */
    public void handleWorkRecordCreation(WorkRecord workRecord) {
        // 양방향 관계 동기화
        workRecord.addToWeeklyAllowance();

        // PENDING_APPROVAL 상태는 WeeklyAllowance 재계산 제외
        if (workRecord.getStatus() != WorkRecordStatus.PENDING_APPROVAL) {
            // WeeklyAllowance의 수당 재계산 (SCHEDULED, COMPLETED만 주휴수당 계산에 포함)
            weeklyAllowanceService.recalculateAllowances(workRecord.getWeeklyAllowance().getId());
        }
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
        // 기존 WeeklyAllowance 수당 재계산 (다른 WeeklyAllowance였다면)
        if (oldWeeklyAllowance != null && !oldWeeklyAllowance.getId().equals(newWeeklyAllowance.getId())) {
            weeklyAllowanceService.recalculateAllowances(oldWeeklyAllowance.getId());
        }

        // 새로운 WeeklyAllowance 수당 재계산
        weeklyAllowanceService.recalculateAllowances(newWeeklyAllowance.getId());

        // COMPLETED 상태일 때만 급여 재계산
        if (workRecord.getStatus() == WorkRecordStatus.COMPLETED) {
            recalculateSalaryForWorkRecord(workRecord);
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
        LocalDate workDate = workRecord.getWorkDate();
        Integer paymentDay = workRecord.getContract().getPaymentDay();

        // workDate가 paymentDay 이상이면 다음 달 급여에 포함, 미만이면 당월 급여에 포함
        Integer year = workDate.getYear();
        Integer month = workDate.getMonthValue();

        if (workDate.getDayOfMonth() >= paymentDay) {
            // 다음 달 급여에 포함
            LocalDate nextMonth = workDate.plusMonths(1);
            year = nextMonth.getYear();
            month = nextMonth.getMonthValue();
        }

        try {
            salaryService.recalculateSalaryAfterWorkRecordUpdate(
                    workRecord.getContract().getId(), year, month);
        } catch (NotFoundException e) {
            // 급여가 아직 생성되지 않은 경우 무시 (정상 케이스)
        }
    }

    /**
     * 특정 날짜에 대한 WeeklyAllowance를 가져오거나 생성
     */
    public WeeklyAllowance getOrCreateWeeklyAllowance(Long contractId, LocalDate workDate) {
        return weeklyAllowanceService.getOrCreateWeeklyAllowanceForDate(contractId, workDate);
    }

    /**
     * 근무 일정 생성 요청 승인 시 주휴수당 재계산
     * PENDING_APPROVAL → SCHEDULED 상태 변경 후 호출
     */
    public void recalculateAllowanceAfterApproval(WorkRecord workRecord) {
        weeklyAllowanceService.recalculateAllowances(workRecord.getWeeklyAllowance().getId());
    }
}
