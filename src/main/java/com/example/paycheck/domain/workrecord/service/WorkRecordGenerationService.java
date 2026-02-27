package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.domain.contract.dto.WorkScheduleDto;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkRecordGenerationService {

    private final WorkRecordRepository workRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 계약 생성 시 2개월치 WorkRecord 생성
     */
    @Transactional
    public void generateInitialWorkRecords(WorkerContract contract) {
        LocalDate startDate = contract.getContractStartDate();
        LocalDate endDate = startDate.plusMonths(2);
        generateWorkRecordsForPeriod(contract, startDate, endDate);
        log.info("초기 2개월치 WorkRecord 생성 완료: Contract ID={}, 기간={} ~ {}", contract.getId(), startDate, endDate);
    }

    /**
     * 2개월 뒤 WorkRecord 생성 (매월 15일 실행)
     * 항상 2개월치 데이터를 유지하기 위해 2개월 뒤의 데이터를 생성
     */
    @Transactional
    public void generateTwoMonthsLaterWorkRecords(WorkerContract contract) {
        // 2개월 뒤의 첫 날부터 마지막 날까지
        LocalDate twoMonthsLaterStart = LocalDate.now().plusMonths(2).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate twoMonthsLaterEnd = twoMonthsLaterStart.with(TemporalAdjusters.lastDayOfMonth());

        generateWorkRecordsForPeriod(contract, twoMonthsLaterStart, twoMonthsLaterEnd);
        log.info("2개월 뒤 WorkRecord 생성 완료: Contract ID={}, 기간={} ~ {}", contract.getId(), twoMonthsLaterStart, twoMonthsLaterEnd);
    }

    /**
     * 특정 기간 동안의 WorkRecord 생성
     */
    @Transactional
    public void generateWorkRecordsForPeriod(WorkerContract contract, LocalDate startDate, LocalDate endDate) {
        List<WorkScheduleDto> schedules = parseWorkSchedules(contract.getWorkSchedules());

        List<WorkRecord> workRecords = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // 계약 종료일이 있는 경우, 종료일 이후는 생성하지 않음
            if (contract.getContractEndDate() != null && currentDate.isAfter(contract.getContractEndDate())) {
                break;
            }

            // 현재 날짜의 요일에 해당하는 스케줄 찾기
            int dayOfWeekValue = currentDate.getDayOfWeek().getValue();
            final LocalDate workDate = currentDate; // 람다에서 사용하기 위한 final 변수

            for (WorkScheduleDto schedule : schedules) {
                if (schedule.getDayOfWeek().equals(dayOfWeekValue)) {
                    // 이미 해당 날짜에 WorkRecord가 있는지 확인
                    boolean exists = workRecordRepository.existsByContractAndWorkDate(contract, workDate);
                    if (!exists) {
                        WorkRecord workRecord = WorkRecord.builder()
                                .contract(contract)
                                .workDate(workDate)
                                .startTime(LocalTime.parse(schedule.getStartTime()))
                                .endTime(LocalTime.parse(schedule.getEndTime()))
                                .breakMinutes(schedule.getBreakMinutes() != null ? schedule.getBreakMinutes() : 0)
                                .status(WorkRecordStatus.SCHEDULED)
                                .build();
                        workRecords.add(workRecord);
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        if (!workRecords.isEmpty()) {
            workRecordRepository.saveAll(workRecords);
            log.info("WorkRecord 생성 완료: {} 개 생성됨 (Contract ID={})", workRecords.size(), contract.getId());
        }
    }

    /**
     * JSON 문자열을 WorkScheduleDto 리스트로 파싱
     */
    private List<WorkScheduleDto> parseWorkSchedules(String workSchedulesJson) {
        try {
            return objectMapper.readValue(workSchedulesJson, new TypeReference<List<WorkScheduleDto>>() {});
        } catch (Exception e) {
            log.error("WorkSchedule 파싱 실패: {}", workSchedulesJson, e);
            throw new RuntimeException("근무 스케줄 파싱 중 오류가 발생했습니다.", e);
        }
    }
}
