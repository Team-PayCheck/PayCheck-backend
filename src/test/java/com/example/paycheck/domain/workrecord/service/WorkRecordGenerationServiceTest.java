package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordGenerationService 단위 테스트")
class WorkRecordGenerationServiceTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WorkRecordGenerationService workRecordGenerationService;

    @Captor
    private ArgumentCaptor<List<WorkRecord>> workRecordsCaptor;

    private WorkerContract contract;

    // 월요일(1)과 수요일(3)에 근무하는 스케줄 JSON
    private static final String WORK_SCHEDULES_JSON =
            "[{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"18:00\",\"breakMinutes\":60}," +
            "{\"dayOfWeek\":3,\"startTime\":\"09:00\",\"endTime\":\"18:00\",\"breakMinutes\":60}]";

    @BeforeEach
    void setUp() {
        // 2026-03-02는 월요일
        contract = WorkerContract.builder()
                .id(1L)
                .hourlyWage(BigDecimal.valueOf(10000))
                .workSchedules(WORK_SCHEDULES_JSON)
                .contractStartDate(LocalDate.of(2026, 3, 2))
                .contractEndDate(null)
                .paymentDay(15)
                .build();
    }

    @Nested
    @DisplayName("generateWorkRecordsForPeriod")
    class GenerateWorkRecordsForPeriod {

        @Test
        @DisplayName("지정된 기간에 대해 스케줄에 맞는 WorkRecord를 생성한다")
        void createsRecordsForFutureDates() {
            // given - 1주간 (월~일)
            LocalDate startDate = LocalDate.of(2026, 3, 2); // 월요일
            LocalDate endDate = LocalDate.of(2026, 3, 8);   // 일요일

            when(workRecordRepository.existsByContractAndWorkDate(eq(contract), any(LocalDate.class)))
                    .thenReturn(false);

            // when
            workRecordGenerationService.generateWorkRecordsForPeriod(contract, startDate, endDate);

            // then
            verify(workRecordRepository).saveAll(workRecordsCaptor.capture());
            List<WorkRecord> savedRecords = workRecordsCaptor.getValue();

            // 월요일(3/2)과 수요일(3/4) = 2개
            assertThat(savedRecords).hasSize(2);
            assertThat(savedRecords.get(0).getWorkDate()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(savedRecords.get(1).getWorkDate()).isEqualTo(LocalDate.of(2026, 3, 4));
        }

        @Test
        @DisplayName("이미 존재하는 날짜에 대해서는 WorkRecord를 생성하지 않는다 (중복 방지)")
        void skipsExistingRecords() {
            // given - 1주간
            LocalDate startDate = LocalDate.of(2026, 3, 2);
            LocalDate endDate = LocalDate.of(2026, 3, 8);

            // 월요일(3/2)에는 이미 레코드가 존재
            when(workRecordRepository.existsByContractAndWorkDate(contract, LocalDate.of(2026, 3, 2)))
                    .thenReturn(true);
            // 수요일(3/4)에는 레코드가 없음
            when(workRecordRepository.existsByContractAndWorkDate(contract, LocalDate.of(2026, 3, 4)))
                    .thenReturn(false);

            // when
            workRecordGenerationService.generateWorkRecordsForPeriod(contract, startDate, endDate);

            // then
            verify(workRecordRepository).saveAll(workRecordsCaptor.capture());
            List<WorkRecord> savedRecords = workRecordsCaptor.getValue();

            // 수요일(3/4)만 생성
            assertThat(savedRecords).hasSize(1);
            assertThat(savedRecords.get(0).getWorkDate()).isEqualTo(LocalDate.of(2026, 3, 4));
        }

        @Test
        @DisplayName("계약 종료일이 있으면 종료일 이후 날짜에는 WorkRecord를 생성하지 않는다")
        void handlesContractEndDate() {
            // given - 계약 종료일이 3/3 (화요일)인 경우
            WorkerContract contractWithEndDate = WorkerContract.builder()
                    .id(2L)
                    .hourlyWage(BigDecimal.valueOf(10000))
                    .workSchedules(WORK_SCHEDULES_JSON)
                    .contractStartDate(LocalDate.of(2026, 3, 2))
                    .contractEndDate(LocalDate.of(2026, 3, 3)) // 화요일까지
                    .paymentDay(15)
                    .build();

            LocalDate startDate = LocalDate.of(2026, 3, 2);
            LocalDate endDate = LocalDate.of(2026, 3, 8);

            when(workRecordRepository.existsByContractAndWorkDate(eq(contractWithEndDate), any(LocalDate.class)))
                    .thenReturn(false);

            // when
            workRecordGenerationService.generateWorkRecordsForPeriod(contractWithEndDate, startDate, endDate);

            // then
            verify(workRecordRepository).saveAll(workRecordsCaptor.capture());
            List<WorkRecord> savedRecords = workRecordsCaptor.getValue();

            // 월요일(3/2)만 생성 (수요일 3/4는 종료일 3/3 이후이므로 제외)
            assertThat(savedRecords).hasSize(1);
            assertThat(savedRecords.get(0).getWorkDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        }

        @Test
        @DisplayName("모든 날짜에 이미 레코드가 존재하면 saveAll을 호출하지 않는다")
        void doesNotSaveWhenAllExist() {
            // given
            LocalDate startDate = LocalDate.of(2026, 3, 2);
            LocalDate endDate = LocalDate.of(2026, 3, 8);

            when(workRecordRepository.existsByContractAndWorkDate(eq(contract), any(LocalDate.class)))
                    .thenReturn(true);

            // when
            workRecordGenerationService.generateWorkRecordsForPeriod(contract, startDate, endDate);

            // then
            verify(workRecordRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("생성된 WorkRecord의 시작/종료 시간과 휴식 시간이 스케줄과 일치한다")
        void workRecordFieldsMatchSchedule() {
            // given
            LocalDate startDate = LocalDate.of(2026, 3, 2); // 월요일
            LocalDate endDate = LocalDate.of(2026, 3, 2);   // 월요일만

            when(workRecordRepository.existsByContractAndWorkDate(eq(contract), any(LocalDate.class)))
                    .thenReturn(false);

            // when
            workRecordGenerationService.generateWorkRecordsForPeriod(contract, startDate, endDate);

            // then
            verify(workRecordRepository).saveAll(workRecordsCaptor.capture());
            WorkRecord record = workRecordsCaptor.getValue().get(0);

            assertThat(record.getStartTime()).hasToString("09:00");
            assertThat(record.getEndTime()).hasToString("18:00");
            assertThat(record.getBreakMinutes()).isEqualTo(60);
            assertThat(record.getStatus()).isEqualTo(com.example.paycheck.domain.workrecord.enums.WorkRecordStatus.SCHEDULED);
        }
    }

    @Nested
    @DisplayName("generateInitialWorkRecords")
    class GenerateInitialWorkRecords {

        @Test
        @DisplayName("계약 시작일부터 2개월치 WorkRecord를 생성한다")
        void generatesTwoMonthsFromStartDate() {
            // given
            when(workRecordRepository.existsByContractAndWorkDate(eq(contract), any(LocalDate.class)))
                    .thenReturn(false);

            // when
            workRecordGenerationService.generateInitialWorkRecords(contract);

            // then
            verify(workRecordRepository).saveAll(workRecordsCaptor.capture());
            List<WorkRecord> savedRecords = workRecordsCaptor.getValue();

            // 2개월간 월/수 근무이므로 약 16~18개 정도 생성
            assertThat(savedRecords).isNotEmpty();

            // 모든 레코드가 계약 시작일 이후 2개월 이내
            LocalDate twoMonthsLater = contract.getContractStartDate().plusMonths(2);
            for (WorkRecord record : savedRecords) {
                assertThat(record.getWorkDate())
                        .isAfterOrEqualTo(contract.getContractStartDate())
                        .isBeforeOrEqualTo(twoMonthsLater);
            }
        }
    }
}
