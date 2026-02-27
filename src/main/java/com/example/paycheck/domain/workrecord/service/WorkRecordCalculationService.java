package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.domain.holiday.service.HolidayService;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * WorkRecord 급여 계산 서비스
 * WorkRecord 엔티티에 서비스 의존성을 주입할 수 없으므로, 별도의 서비스로 계산 로직을 중앙화
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkRecordCalculationService {

    private final HolidayService holidayService;

    /**
     * WorkRecord의 시간 및 급여를 계산
     *
     * @param workRecord 계산할 WorkRecord
     */
    public void calculateWorkRecordDetails(WorkRecord workRecord) {
        // 1. 사업장 규모 확인
        Workplace workplace = workRecord.getContract().getWorkplace();
        boolean isSmallWorkplace = workplace.getIsLessThanFiveEmployees();

        // 2. 휴일 여부 확인 (주말 + 공공기관 휴일)
        boolean isHoliday = holidayService.isPublicHoliday(workRecord.getWorkDate());

        // 3. 로그 출력 (디버깅용)
        if (log.isDebugEnabled()) {
            log.debug("WorkRecord 계산: workDate={}, isPublicHoliday={}, isSmallWorkplace={}",
                    workRecord.getWorkDate(), isHoliday, isSmallWorkplace);
        }

        // 4. 엔티티 계산 메서드 호출
        workRecord.calculateHoursWithHolidayInfo(isHoliday, isSmallWorkplace);
        workRecord.calculateSalaryWithAllowanceRules(isSmallWorkplace);
    }

    /**
     * WorkRecord 계산 결과 정합성 검증
     */
    public void validateWorkRecordConsistency(WorkRecord workRecord) {
        Integer totalWorkMinutes = workRecord.getTotalWorkMinutes();
        if (totalWorkMinutes == null || totalWorkMinutes < 0) {
            throw new IllegalStateException(
                    String.format("WorkRecord 필드 정합성 검증 실패: totalWorkMinutes=%s", totalWorkMinutes));
        }

        validateNotNegative("totalHours", workRecord.getTotalHours());
        validateNotNegative("baseSalary", workRecord.getBaseSalary());
        validateNotNegative("nightSalary", workRecord.getNightSalary());
        validateNotNegative("holidaySalary", workRecord.getHolidaySalary());
        validateNotNegative("totalSalary", workRecord.getTotalSalary());

        BigDecimal expectedTotalSalary = workRecord.getBaseSalary()
                .add(workRecord.getNightSalary())
                .add(workRecord.getHolidaySalary());

        if (workRecord.getTotalSalary().compareTo(expectedTotalSalary) != 0) {
            throw new IllegalStateException(String.format(
                    "WorkRecord 급여 정합성 검증 실패: workRecordId=%d, totalSalary=%s, expected=%s",
                    workRecord.getId(), workRecord.getTotalSalary(), expectedTotalSalary));
        }
    }

    private void validateNotNegative(String fieldName, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    String.format("WorkRecord 필드 정합성 검증 실패: %s=%s", fieldName, value));
        }
    }
}
