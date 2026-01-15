package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.domain.holiday.service.HolidayService;
import com.example.wagemanager.domain.workplace.entity.Workplace;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        // 2. 휴일 여부 확인 (주말 + 법정공휴일)
        boolean isHoliday = holidayService.isHoliday(workRecord.getWorkDate());

        // 3. 로그 출력 (디버깅용)
        if (log.isDebugEnabled()) {
            log.debug("WorkRecord 계산: workDate={}, isHoliday={}, isSmallWorkplace={}",
                    workRecord.getWorkDate(), isHoliday, isSmallWorkplace);
        }

        // 4. 엔티티 계산 메서드 호출
        workRecord.calculateHoursWithHolidayInfo(isHoliday, isSmallWorkplace);
        workRecord.calculateSalaryWithAllowanceRules(isSmallWorkplace);
    }
}
